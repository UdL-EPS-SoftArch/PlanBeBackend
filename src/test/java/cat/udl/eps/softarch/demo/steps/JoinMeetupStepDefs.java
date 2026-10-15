package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
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
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "organizer", null, java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        try {
            Meetup meetup = new Meetup();
            meetup.setTitle("Test Meetup");
            meetup.setCapacity(capacity);
            currentMeetup = meetupRepository.save(meetup);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
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

    @Then("the meetup status should be {string}")
    public void the_meetup_status_should_be(String status) {
        Meetup updatedMeetup = meetupRepository.findAll().stream()
                .filter(m -> m.getId().equals(currentMeetup.getId()))
                .findFirst().orElseThrow();
        assertEquals(MeetupStatus.valueOf(status), updatedMeetup.getStatus());
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
