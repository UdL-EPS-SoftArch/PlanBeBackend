package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.enums.Visibility;
import cat.udl.eps.softarch.demo.handler.WaitingList;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Steps of WaitingList.feature (previously a second class inside JoinMeetupStepDefs.java). */
public class WaitingListStepDefs {

    private final StepDefs stepDefs;
    private final MeetupRepository meetupRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final WaitingList waitingList;
    private final EntityManagerFactory entityManagerFactory;
    private final JdbcTemplate jdbcTemplate;

    private Meetup currentMeetup;

    public WaitingListStepDefs(StepDefs stepDefs, MeetupRepository meetupRepository,
                               ParticipationRepository participationRepository, UserRepository userRepository,
                               WaitingList waitingList, EntityManagerFactory entityManagerFactory,
                               JdbcTemplate jdbcTemplate) {
        this.stepDefs = stepDefs;
        this.meetupRepository = meetupRepository;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
        this.waitingList = waitingList;
        this.entityManagerFactory = entityManagerFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Given("a meetup exists with capacity {int}")
    public void aMeetupExistsWithCapacity(Integer capacity) {
        Meetup meetup = new Meetup();
        meetup.setTitle("Test Meetup");
        meetup.setCapacity(capacity);
        meetup.setVisibility(Visibility.PUBLIC);
        meetup.setStatus(MeetupStatus.OPEN);
        currentMeetup = RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @Given("{int} user(s) have already joined the meetup")
    @Given("{int} user has already joined the meetup")
    public void usersHaveAlreadyJoinedTheMeetup(Integer count) {
        for (int i = 0; i < count; i++) {
            addParticipation("user" + i, ParticipationStatus.CONFIRMED);
        }
        if (count >= currentMeetup.getCapacity()) {
            currentMeetup.setStatus(MeetupStatus.FULL);
            currentMeetup = RunAs.admin(() -> meetupRepository.save(currentMeetup));
        }
    }

    @Given("another user is on the waiting list")
    public void anotherUserIsOnTheWaitingList() {
        addParticipation("waitlisted", ParticipationStatus.WAITING);
    }

    @When("I join the meetup")
    public void iJoinTheMeetup() throws Exception {
        existingUser("me");
        String json = "{\"meetup\": \"/meetups/" + currentMeetup.getId() + "\"}";
        stepDefs.result = stepDefs.mockMvc.perform(post("/participations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(user("me")));
    }

    @When("a participant leaves the meetup")
    public void aParticipantLeavesTheMeetup() throws Exception {
        Participation p = participationRepository.findAll().stream()
                .filter(p1 -> p1.getMeetup().getId().equals(currentMeetup.getId()))
                .filter(p1 -> p1.getStatus() == ParticipationStatus.CONFIRMED)
                .min(Comparator.comparing(Participation::getId))
                .orElseThrow();
        stepDefs.result = stepDefs.mockMvc.perform(delete("/participations/" + p.getId())
                .with(user(p.getParticipant().getId())));
    }

    /**
     * Reproduces, step by step, what open-in-view does in POST /participations: the request's persistence
     * context loads the meetup (to resolve "/meetups/{id}"), another request cancels it and commits, and then
     * the waiting list is filled in the first request. The decision must use the committed state.
     */
    @When("The meetup {string} is cancelled by another request while its waiting list is being filled")
    public void theMeetupIsCancelledWhileItsWaitingListIsBeingFilled(String title) {
        Long meetupId = meetupRepository.findByTitle(title).orElseThrow().getId();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManager));
        try {
            entityManager.find(Meetup.class, meetupId);   // loaded as OPEN by "this" request
            jdbcTemplate.update("update meetup set status = 'CANCELLED' where id = ?", meetupId);   // the other one
            waitingList.fillFreeSpots(meetupId);
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
            entityManager.close();
        }
    }

    @Then("the meetup status should be {string}")
    public void theMeetupStatusShouldBe(String status) {
        Meetup updatedMeetup = meetupRepository.findById(currentMeetup.getId()).orElseThrow();
        assertEquals(MeetupStatus.valueOf(status), updatedMeetup.getStatus());
    }

    @Then("my participation status should be {string}")
    public void myParticipationStatusShouldBe(String status) {
        assertEquals(ParticipationStatus.valueOf(status), statusOf("me"));
    }

    @Then("the waiting user status should be {string}")
    public void theWaitingUserStatusShouldBe(String status) {
        assertEquals(ParticipationStatus.valueOf(status), statusOf("waitlisted"));
    }

    private ParticipationStatus statusOf(String username) {
        return participationRepository.findByParticipantIdAndMeetupId(username, currentMeetup.getId())
                .orElseThrow().getStatus();
    }

    private void addParticipation(String username, ParticipationStatus status) {
        Participation p = new Participation();
        p.setParticipant(existingUser(username));
        p.setMeetup(currentMeetup);
        p.setStatus(status);
        p.setJoinedAt(Instant.now());
        participationRepository.save(p);
    }

    private User existingUser(String username) {
        return userRepository.findById(username).orElseGet(() -> {
            User user = new User();
            user.setId(username);
            user.setEmail(username + "@test.com");
            user.setPassword("password");
            user.encodePassword();
            return userRepository.save(user);
        });
    }
}
