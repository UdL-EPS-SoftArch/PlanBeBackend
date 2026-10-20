package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.*;
import cat.udl.eps.softarch.demo.repository.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class JoinMeetupStepDefs {

    private final StepDefs stepDefs;
    private final MeetupRepository meetupRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private Meetup currentMeetup;

    public JoinMeetupStepDefs(StepDefs stepDefs, MeetupRepository meetupRepository, UserRepository userRepository,
                              ParticipationRepository participationRepository) {
        this.stepDefs = stepDefs;
        this.meetupRepository = meetupRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        // Ensure the user exists in the database so the EventHandler doesn't throw 404
        if (!userRepository.existsById(username)) {
            User user = new User();
            user.setId(username);
            user.setPassword("password123");
            user.setEmail(username + "@sample.app");
            user.encodePassword();
            userRepository.save(user);
        }
    }

    @Given("the following meetups exist:")
    public void theFollowingMeetupsExist(io.cucumber.datatable.DataTable dataTable) {
        dataTable.asMaps(String.class, String.class).forEach(row -> {
            Meetup meetup = new Meetup();
            meetup.setTitle(row.get("title"));
            meetup.setCapacity(Integer.parseInt(row.get("capacity")));
            meetup.setVisibility(Meetup.Visibility.valueOf(row.get("visibility")));
            meetup.setStatus(Meetup.MeetupStatus.valueOf(row.get("status")));
            RunAs.admin(() -> meetupRepository.save(meetup));
        });
    }

    @Given("the meetup {string} is already full")
    public void theMeetupIsAlreadyFull(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        int capacity = meetup.getCapacity();
        for (int i = 0; i < capacity; i++) {
            String username = "full_user_" + i;
            User user = userRepository.findById(username).orElseGet(() -> {
                User newUser = new User();
                newUser.setId(username);
                newUser.setPassword("password123");
                newUser.setEmail(username + "@sample.app");
                newUser.encodePassword();
                return userRepository.save(newUser);
            });

            Participation p = new Participation();
            p.setParticipant(user);
            p.setMeetup(meetup);
            p.setStatus(ParticipationStatus.CONFIRMED);
            p.setJoinedAt(java.time.Instant.now());
            participationRepository.save(p);
        }
    }

    @Given("I have already joined the meetup {string}")
    @Given("I have joined the meetup {string}")
    public void iHaveJoinedTheMeetup(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        // Assuming "user1" is the current user for these scenarios
        User user = userRepository.findById("user1").orElseGet(() -> {
            User newUser = new User();
            newUser.setId("user1");
            newUser.setPassword("password123");
            newUser.setEmail("user1@sample.app");
            newUser.encodePassword();
            return userRepository.save(newUser);
        });

        Participation p = new Participation();
        p.setParticipant(user);
        p.setMeetup(meetup);
        p.setStatus(ParticipationStatus.CONFIRMED);
        p.setJoinedAt(java.time.Instant.now());
        participationRepository.save(p);
    }

    @When("I join the meetup {string}")
    public void iJoinTheMeetup(String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        String body = "{\"meetup\": \"/meetups/" + meetup.getId() + "\" }";

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.post("/participations")
                .with(SecurityMockMvcRequestPostProcessors.user("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("I leave the meetup {string}")
    public void iLeaveTheMeetup(String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        Optional<Participation> p = participationRepository.findByParticipantIdAndMeetupId("user1", meetup.getId());

        if (p.isEmpty()) {
            throw new RuntimeException("Participation not found for user1 and meetup " + title);
        }

        String body = "{\"status\": \"CANCELLED\"}";

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.patch("/participations/" + p.get().getId())
                .with(SecurityMockMvcRequestPostProcessors.user("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Given("a meetup exists with capacity {int}")
    public void aMeetupExistsWithCapacity(Integer capacity) {
        Meetup meetup = new Meetup();
        meetup.setTitle("Test Meetup");
        meetup.setCapacity(capacity);
        meetup.setVisibility(Meetup.Visibility.PUBLIC);
        meetup.setStatus(Meetup.MeetupStatus.OPEN);
        currentMeetup = RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @Given("{int} user(s) have already joined the meetup")
    @Given("{int} user has already joined the meetup")
    public void usersHaveAlreadyJoinedTheMeetup(Integer count) {
        for (int i = 0; i < count; i++) {
            User user = userRepository.findById("user" + i).orElseGet(() -> {
                User newUser = new User();
                newUser.setId("user" + i);
                newUser.setPassword("password123");
                newUser.setEmail("user" + i + "@sample.app");
                newUser.encodePassword();
                return userRepository.save(newUser);
            });

            Participation participation = new Participation();
            participation.setParticipant(user);
            participation.setMeetup(currentMeetup);
            participation.setStatus(ParticipationStatus.CONFIRMED);
            participation.setJoinedAt(Instant.now());
            participationRepository.save(participation);
        }
    }

    @When("I join the meetup")
    public void iJoinTheCurrentMeetup() throws Exception {
        iAmLoggedInAs("me");
        String body = "{\"meetup\": \"/meetups/" + currentMeetup.getId() + "\" }";
        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.post("/participations")
            .with(SecurityMockMvcRequestPostProcessors.user("me"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    @When("a participant leaves the meetup")
    public void aParticipantLeavesTheMeetup() throws Exception {
        Participation participation = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
            .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(currentMeetup.getId()))
            .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
            .findFirst()
            .orElseThrow();

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.delete("/participations/" + participation.getId())
            .with(SecurityMockMvcRequestPostProcessors.user(participation.getParticipant().getId())));
    }

    @Then("the meetup status should be {string}")
    public void theMeetupStatusShouldBe(String status) {
        Meetup updatedMeetup = meetupRepository.findById(currentMeetup.getId()).orElseThrow();
        if (!updatedMeetup.getStatus().name().equals(status)) {
            throw new AssertionError("Expected meetup status " + status + " but was " + updatedMeetup.getStatus().name());
        }
    }

    @Then("the response code is {int}")
    public void theResponseCodeIs(int code) throws Exception {
        if (code == 200) {
            // Allow 204 No Content for PATCH/DELETE operations in Spring Data REST
            stepDefs.result.andExpect(result -> {
                int actualStatus = result.getResponse().getStatus();
                if (actualStatus != 200 && actualStatus != 204) {
                    throw new AssertionError("Expected status 200 or 204 but was " + actualStatus);
                }
            });
        } else {
            stepDefs.result.andExpect(status().is(code));
        }
    }

    @Then("the participation status is {string}")
    public void theParticipationStatusIs(String status) {
        // We verify the status directly in the database (DatabaseCleanupHooks guarantees one meetup per user1
        // scenario). A user who left and joined again keeps the old row, so the current one is the latest.
        Participation p = participationRepository.findAll().stream()
            .filter(participation -> participation.getParticipant().getId().equals("user1"))
            .max(java.util.Comparator.comparing(Participation::getId))
            .orElseThrow(() -> new RuntimeException("No participation found for user1 to verify status"));

        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }

    @Then("my earlier participation in the meetup {string} is kept as {string}")
    public void myEarlierParticipationIsKeptAs(String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        java.util.List<Participation> history =
            participationRepository.findByParticipantIdAndMeetupIdOrderByIdAsc("user1", meetup.getId());
        if (history.size() != 2 || !history.get(0).getStatus().name().equals(status)) {
            throw new AssertionError("Expected 2 participations, the first one " + status + ", but got "
                + history.stream().map(p -> p.getStatus().name()).toList());
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
