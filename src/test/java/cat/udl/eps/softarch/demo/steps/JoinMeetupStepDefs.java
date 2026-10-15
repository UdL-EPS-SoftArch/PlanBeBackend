package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.*;
import cat.udl.eps.softarch.demo.repository.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

public class JoinMeetupStepDefs {

    private final WebApplicationContext wac;
    private final MeetupRepository meetupRepository;
    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private MockMvc mockMvc;
    private ResultActions result;

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

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        if (this.mockMvc == null) setup();

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
            User user = new User();
            user.setId("full_user_" + i);
            user.setPassword("password123");
            user.setEmail("full_user_" + i + "@sample.app");
            user.encodePassword();
            userRepository.save(user);

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

    @Then("the response code is {int}")
    public void theResponseCodeIs(int code) throws Exception {
        if (code == 200) {
            // Allow 204 No Content for PATCH/DELETE operations in Spring Data REST
            result.andExpect(result -> {
                int actualStatus = result.getResponse().getStatus();
                if (actualStatus != 200 && actualStatus != 204) {
                    throw new AssertionError("Expected status 200 or 204 but was " + actualStatus);
                }
            });
        } else {
            result.andExpect(status().is(code));
        }
    }

    @Then("the participation status is {string}")
    public void theParticipationStatusIs(String status) throws Exception {
        // We verify the status directly in the database to be independent of the HTTP response body
        // We assume "user1" is the participant for these tests
        // We need to find the participation for user1 in any meetup (or the last one created)
        // For the sake of these tests, we'll look for the first active participation of user1
        Participation p = participationRepository.findAll().stream()
            .filter(participation -> participation.getParticipant().getId().equals("user1"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No participation found for user1 to verify status"));

        if (!p.getStatus().name().equals(status)) {
            throw new AssertionError("Expected status " + status + " but was " + p.getStatus().name());
        }
    }
}
package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.stream.StreamSupport;
import java.util.Comparator;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class JoinMeetupStepDefs {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private MeetupRepository meetupRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private UserRepository userRepository;

    private Meetup currentMeetup;

    @Given("a meetup exists with capacity {int}")
    public void a_meetup_exists_with_capacity(Integer capacity) throws Exception {
        Meetup meetup = new Meetup();
        meetup.setTitle("Test Meetup");
        meetup.setCapacity(capacity);
        currentMeetup = meetupRepository.save(meetup);
    }

    @Given("{int} user(s) have already joined the meetup")
    @Given("{int} user has already joined the meetup")
    public void users_have_already_joined_the_meetup(Integer count) throws Exception {
        for (int i = 0; i < count; i++) {
            User user = new User();
            String username = "user" + i;
            user.setId(username);
            user.setEmail(username + "@test.com");
            user.setPassword("password");
            userRepository.save(user);

            Participation p = new Participation();
            p.setUser(user);
            p.setMeetup(currentMeetup);
            participationRepository.save(p);
        }
    }

    @When("I join the meetup")
    public void i_join_the_meetup() throws Exception {
        User user = new User();
        String username = "me";
        user.setId(username);
        user.setEmail("me@test.com");
        user.setPassword("password");
        userRepository.save(user);

        String userUri = "/users/" + user.getId();
        String meetupUri = "/meetups/" + currentMeetup.getId();

        String json = String.format("{\"user\": \"%s\", \"meetup\": \"%s\"}", userUri, meetupUri);

        stepDefs.result = stepDefs.mockMvc.perform(post("/participations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @When("a participant leaves the meetup")
    public void a_participant_leaves_the_meetup() throws Exception {
        Participation p = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                .findFirst()
                .orElseThrow();

        // To avoid 401 Unauthorized, we must perform the request as the user who owns the participation
        // In a real scenario, we would use .with(user(p.getUser()))
        stepDefs.result = stepDefs.mockMvc.perform(delete("/participations/" + p.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(p.getUser().getId())));
    }

    @Then("the response code is {int}")
    public void the_response_code_is(Integer code) throws Throwable {
        stepDefs.theResponseCodeIs(code);
    }

    @Then("the meetup status should be {string}")
    public void the_meetup_status_should_be(String status) {
        Meetup updatedMeetup = meetupRepository.findById(currentMeetup.getId()).orElseThrow();
        assertEquals(Meetup.MeetupStatus.valueOf(status), updatedMeetup.getStatus());
    }

    @Then("my participation status should be {string}")
    public void my_participation_status_should_be(String status) {
        Participation p = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                .filter(p1 -> p1.getUser() != null && p1.getUser().getId().equals("me"))
                .findFirst()
                .orElseThrow();
        assertEquals(Participation.ParticipationStatus.valueOf(status), p.getStatus());
    }

    @Given("another user is on the waiting list")
    public void another_user_is_on_the_waiting_list() throws Exception {
        User user = new User();
        String username = "waitlisted";
        user.setId(username);
        user.setEmail(username + "@test.com");
        user.setPassword("password");
        userRepository.save(user);

        Participation p = new Participation();
        p.setUser(user);
        p.setMeetup(currentMeetup);
        p.setStatus(Participation.ParticipationStatus.WAITING);
        participationRepository.save(p);
    }

    @Then("the waiting user status should be {string}")
    public void the_waiting_user_status_should_be(String status) {
        Participation p = StreamSupport.stream(participationRepository.findAll().spliterator(), false)
                .filter(p1 -> p1.getUser() != null && p1.getUser().getId().equals("waitlisted"))
                .findFirst()
                .orElseThrow();
        assertEquals(Participation.ParticipationStatus.valueOf(status), p.getStatus());
    }
}
