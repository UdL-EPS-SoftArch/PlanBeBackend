package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.*;
import cat.udl.eps.softarch.demo.repository.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JoinMeetupStepDefs {

    private final WebApplicationContext wac;
    private final MeetupRepository meetupRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private MockMvc mockMvc;
    private ResultActions result;

    private Meetup currentMeetup;

    public JoinMeetupStepDefs(WebApplicationContext wac, MeetupRepository meetupRepository, UserRepository userRepository, ParticipationRepository participationRepository) {
        this.wac = wac;
        this.meetupRepository = meetupRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private User ensureUser(String username) {
        return userRepository.findById(username).orElseGet(() -> {
            User user = new User();
            user.setId(username);
            user.setPassword("password123");
            user.setEmail(username + "@sample.app");
            user.encodePassword();
            return userRepository.save(user);
        });
    }

    // ---------- Steps of the public join/leave feature ----------

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        if (this.mockMvc == null) setup();
        ensureUser(username);
    }

    @Given("the following meetups exist:")
    public void theFollowingMeetupsExist(io.cucumber.datatable.DataTable dataTable) {
        if (this.mockMvc == null) setup();

        dataTable.asMaps(String.class, String.class).forEach(row -> {
            Meetup meetup = new Meetup();
            meetup.setTitle(row.get("title"));
            meetup.setCapacity(Integer.parseInt(row.get("capacity")));
            meetup.setVisibility(Meetup.Visibility.valueOf(row.get("visibility")));
            meetup.setStatus(Meetup.MeetupStatus.valueOf(row.get("status")));
            meetupRepository.save(meetup);
        });
    }

    @Given("the meetup {string} is already full")
    public void theMeetupIsAlreadyFull(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        int capacity = meetup.getCapacity();
        for (int i = 0; i < capacity; i++) {
            User user = ensureUser("full_user_" + i);

            Participation p = new Participation();
            p.setParticipant(user);
            p.setMeetup(meetup);
            p.setStatus(ParticipationStatus.CONFIRMED);
            p.setJoinedAt(Instant.now());
            participationRepository.save(p);
        }
    }

    @Given("I have already joined the meetup {string}")
    @Given("I have joined the meetup {string}")
    public void iHaveJoinedTheMeetup(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        User user = ensureUser("user1");

        Participation p = new Participation();
        p.setParticipant(user);
        p.setMeetup(meetup);
        p.setStatus(ParticipationStatus.CONFIRMED);
        p.setJoinedAt(Instant.now());
        participationRepository.save(p);
    }

    @When("I join the meetup {string}")
    public void iJoinTheMeetup(String title) throws Exception {
        if (this.mockMvc == null) setup();
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        String body = "{\"meetup\": \"/meetups/" + meetup.getId() + "\" }";

        this.result = mockMvc.perform(MockMvcRequestBuilders.post("/participations")
                .with(SecurityMockMvcRequestPostProcessors.user("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("I leave the meetup {string}")
    public void iLeaveTheMeetup(String title) throws Exception {
        if (this.mockMvc == null) setup();
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Optional<Participation> p = participationRepository.findByParticipantIdAndMeetupId("user1", meetup.getId());

        if (p.isEmpty()) {
            throw new RuntimeException("Participation not found for user1 and meetup " + title);
        }

        String body = "{\"status\": \"CANCELLED\"}";

        this.result = mockMvc.perform(MockMvcRequestBuilders.patch("/participations/" + p.get().getId())
                .with(SecurityMockMvcRequestPostProcessors.user("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Then("the participation status is {string}")
    public void theParticipationStatusIs(String status) {
        Participation p = participationRepository.findAll().stream()
            .filter(participation -> participation.getParticipant() != null
                && participation.getParticipant().getId().equals("user1"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No participation found for user1 to verify status"));

        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }

    // ---------- Steps of the capacity / waiting list feature ----------

    @Given("a meetup exists with capacity {int}")
    public void aMeetupExistsWithCapacity(Integer capacity) {
        if (this.mockMvc == null) setup();
        Meetup meetup = new Meetup();
        meetup.setTitle("Test Meetup");
        meetup.setCapacity(capacity);
        meetup.setVisibility(Meetup.Visibility.PUBLIC);
        meetup.setStatus(Meetup.MeetupStatus.OPEN);
        currentMeetup = meetupRepository.save(meetup);
    }

    @Given("{int} user(s) have already joined the meetup")
    @Given("{int} user has already joined the meetup")
    public void usersHaveAlreadyJoinedTheMeetup(Integer count) {
        for (int i = 0; i < count; i++) {
            User user = ensureUser("user" + i);

            Participation p = new Participation();
            p.setParticipant(user);
            p.setMeetup(currentMeetup);
            p.setStatus(ParticipationStatus.CONFIRMED);
            p.setJoinedAt(Instant.now());
            participationRepository.save(p);
        }
    }

    @When("I join the meetup")
    public void iJoinTheCurrentMeetup() throws Exception {
        if (this.mockMvc == null) setup();
        ensureUser("me");

        String body = "{\"meetup\": \"/meetups/" + currentMeetup.getId() + "\" }";

        this.result = mockMvc.perform(MockMvcRequestBuilders.post("/participations")
                .with(SecurityMockMvcRequestPostProcessors.user("me"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("a participant leaves the meetup")
    public void aParticipantLeavesTheMeetup() throws Exception {
        if (this.mockMvc == null) setup();

        Participation p = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
            .filter(participation -> participation.getMeetup() != null
                && participation.getMeetup().getId().equals(currentMeetup.getId()))
            .filter(participation -> participation.getStatus() == ParticipationStatus.CONFIRMED)
            .findFirst()
            .orElseThrow();

        this.result = mockMvc.perform(MockMvcRequestBuilders.delete("/participations/" + p.getId())
                .with(SecurityMockMvcRequestPostProcessors.user(p.getParticipant().getId())));
    }

    @Given("another user is on the waiting list")
    public void anotherUserIsOnTheWaitingList() {
        User user = ensureUser("waitlisted");

        Participation p = new Participation();
        p.setParticipant(user);
        p.setMeetup(currentMeetup);
        p.setStatus(ParticipationStatus.WAITING);
        p.setJoinedAt(Instant.now());
        participationRepository.save(p);
    }

    @Then("the meetup status should be {string}")
    public void theMeetupStatusShouldBe(String status) {
        Meetup updatedMeetup = meetupRepository.findById(currentMeetup.getId()).orElseThrow();
        if (!updatedMeetup.getStatus().name().equals(status)) {
            throw new AssertionError("Expected meetup status " + status + " but was " + updatedMeetup.getStatus().name());
        }
    }

    @Then("my participation status should be {string}")
    public void myParticipationStatusShouldBe(String status) {
        Participation p = findParticipationFor("me");
        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }

    @Then("the waiting user status should be {string}")
    public void theWaitingUserStatusShouldBe(String status) {
        Participation p = findParticipationFor("waitlisted");
        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }

    private Participation findParticipationFor(String username) {
        return StreamSupport.stream(participationRepository.findAll().spliterator(), false)
            .filter(participation -> participation.getParticipant() != null
                && participation.getParticipant().getId().equals(username))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No participation found for " + username));
    }

    @Then("the response code is {int}")
    public void theResponseCodeIs(int code) throws Exception {
        if (code == 200) {
            // Allow 204 No Content for PATCH/DELETE operations in Spring Data REST
            result.andExpect(res -> {
                int actualStatus = res.getResponse().getStatus();
                if (actualStatus != 200 && actualStatus != 204) {
                    throw new AssertionError("Expected status 200 or 204 but was " + actualStatus);
                }
            });
        } else {
            result.andExpect(status().is(code));
        }
    }

    // ---------- Steps of the cancel meetup feature ----------

    @Given("the meetup {string} is cancelled")
    public void theMeetupIsCancelled(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        meetup.setStatus(Meetup.MeetupStatus.CANCELLED);
        meetupRepository.save(meetup);
    }

    @When("the organizer cancels the meetup {string}")
    public void theOrganizerCancelsTheMeetup(String title) throws Exception {
        if (this.mockMvc == null) setup();
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        ensureUser("user1");

        String body = "{\"status\": \"CANCELLED\"}";

        this.result = mockMvc.perform(MockMvcRequestBuilders.patch("/meetups/" + meetup.getId())
                .with(SecurityMockMvcRequestPostProcessors.user("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
