package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.enums.Visibility;
import cat.udl.eps.softarch.demo.steps.RunAs;
import cat.udl.eps.softarch.demo.domain.*;
import cat.udl.eps.softarch.demo.repository.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
    /** User whose participation the scenario is about: the one logged in when joining/leaving. */
    private String participantUsername;

    public JoinMeetupStepDefs(StepDefs stepDefs, MeetupRepository meetupRepository, UserRepository userRepository,
                              ParticipationRepository participationRepository) {
        this.stepDefs = stepDefs;
        this.meetupRepository = meetupRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
    }

    private String currentUsername() {
        return AuthenticationStepDefs.currentUsername != null && !AuthenticationStepDefs.currentUsername.isEmpty()
            ? AuthenticationStepDefs.currentUsername
            : stepDefs.currentUser;
    }

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
        String username = currentUsername();
        User user = userRepository.findById(username)
            .orElseThrow(() -> new IllegalStateException("No logged in user to join the meetup"));

        Participation p = new Participation();
        p.setParticipant(user);
        p.setMeetup(meetup);
        p.setStatus(ParticipationStatus.CONFIRMED);
        p.setJoinedAt(java.time.Instant.now());
        participationRepository.save(p);
        participantUsername = username;
    }

    @When("I join the meetup {string}")
    public void iJoinTheMeetup(String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        String body = "{\"meetup\": \"/meetups/" + meetup.getId() + "\" }";

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.post("/participations")
                .with(AuthenticationStepDefs.authenticate())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        participantUsername = currentUsername();
    }

    @When("I leave the meetup {string}")
    public void iLeaveTheMeetup(String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        String username = currentUsername();
        Optional<Participation> p = participationRepository.findByParticipantIdAndMeetupId(username, meetup.getId());

        if (p.isEmpty()) {
            throw new RuntimeException("Participation not found for " + username + " and meetup " + title);
        }

        String body = "{\"status\": \"CANCELLED\"}";

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.patch("/participations/" + p.get().getId())
                .with(AuthenticationStepDefs.authenticate())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
        participantUsername = username;
    }

    public void aMeetupExistsWithCapacityForJoin(Integer capacity) {
        Meetup meetup = new Meetup();
        meetup.setTitle("Test Meetup");
        meetup.setCapacity(capacity);
        meetup.setVisibility(Visibility.PUBLIC);
        meetup.setStatus(MeetupStatus.OPEN);
        currentMeetup = RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @Given("the meetup {string} is already full")
    public void the_meetup_is_already_full(String title) {
        theMeetupIsAlreadyFull(title);
    }

    @Then("the participation status is {string}")
    public void the_participation_status_is(String status) {
        // We verify the status directly in the database. A user who left and joined again keeps the old row,
        // so the current one is the latest.
        String username = participantUsername != null ? participantUsername : currentUsername();
        Participation p = participationRepository.findAll().stream()
            .filter(participation -> participation.getParticipant().getId().equals(username))
            .max(java.util.Comparator.comparing(Participation::getId))
            .orElseThrow(() -> new RuntimeException("No participation found for " + username + " to verify status"));

        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }

    @Then("my earlier participation in the meetup {string} is kept as {string}")
    public void my_earlier_participation_in_the_meetup_is_kept_as(String title, String status) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        String username = participantUsername != null ? participantUsername : currentUsername();
        java.util.List<Participation> history =
            participationRepository.findByParticipantIdAndMeetupIdOrderByIdAsc(username, meetup.getId());
        if (history.size() != 2 || !history.get(0).getStatus().name().equals(status)) {
            throw new AssertionError("Expected 2 participations, the first one " + status + ", but got "
                + history.stream().map(p -> p.getStatus().name()).toList());
        }
    }

    // ---------- Steps of the cancel meetup feature ----------

    @Given("the meetup {string} is cancelled")
    public void theMeetupIsCancelled(String title) {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();
        meetup.setStatus(MeetupStatus.CANCELLED);
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @When("the organizer cancels the meetup {string}")
    public void theOrganizerCancelsTheMeetup(String title) throws Exception {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();

        String body = "{\"status\": \"CANCELLED\"}";

        stepDefs.result = stepDefs.mockMvc.perform(MockMvcRequestBuilders.patch("/meetups/" + meetup.getId())
                .with(AuthenticationStepDefs.authenticate())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
