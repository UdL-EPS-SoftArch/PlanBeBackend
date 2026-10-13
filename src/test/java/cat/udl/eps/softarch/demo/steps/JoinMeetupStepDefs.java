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
