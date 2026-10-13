package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.time.Instant;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MeetupStepDefs {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetupRepository meetupRepository;

    @Given("the following users exist:")
    public void the_following_users_exist(io.cucumber.datatable.DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            User user = new User();
            user.setId(row.get("username"));
            user.setPassword(User.passwordEncoder.encode(row.get("password")));
            user.setEmail(row.get("username") + "@example.com");
            userRepository.save(user);
        }
    }

    @When("I create a new meetup with the following details:")
    public void i_create_a_new_meetup_with_the_following_details(io.cucumber.datatable.DataTable dataTable) throws Throwable {
        Map<String, String> details = dataTable.asMap(String.class, String.class);
        Map<String, Object> body = new HashMap<>();
        
        if (details.containsKey("title")) body.put("title", details.get("title"));
        if (details.containsKey("description")) body.put("description", details.get("description"));
        if (details.containsKey("capacity")) body.put("capacity", Integer.parseInt(details.get("capacity")));
        if (details.containsKey("startDate")) body.put("startDate", details.get("startDate"));
        if (details.containsKey("endDate")) body.put("endDate", details.get("endDate"));
        
        if (details.containsKey("locName") || details.containsKey("locAddress") || details.containsKey("locCity") || details.containsKey("locLat") || details.containsKey("locLon")) {
            Map<String, Object> location = new HashMap<>();
            if (details.containsKey("locName")) location.put("name", details.get("locName"));
            if (details.containsKey("locAddress")) location.put("address", details.get("locAddress"));
            if (details.containsKey("locCity")) location.put("city", details.get("locCity"));
            if (details.containsKey("locLat")) location.put("latitude", Double.parseDouble(details.get("locLat")));
            if (details.containsKey("locLon")) location.put("longitude", Double.parseDouble(details.get("locLon")));
            body.put("location", location);
        }

        stepDefs.result = stepDefs.mockMvc.perform(post("/meetups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(body))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("organizer").roles("USER")));
    }

    @Then("The meetup {string} is organized by {string}")
    public void the_meetup_is_organized_by(String title, String username) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')]._links.organizer").exists());
    }

    @Given("a meetup {string} exists organized by {string}")
    public void a_meetup_exists_organized_by(String title, String username) {
        User organizer = userRepository.findById(username).orElseGet(() -> {
            User u = new User();
            u.setId(username);
            u.setPassword(User.passwordEncoder.encode("password"));
            u.setEmail(username + "@example.com");
            return userRepository.save(u);
        });

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
        );
        try {
            Meetup meetup = new Meetup();
            meetup.setTitle(title);
            meetup.setOrganizer(organizer);
            meetup.setStartDate(Instant.now().plusSeconds(3600));
            meetup.setEndDate(Instant.now().plusSeconds(7200));
            meetup.setCapacity(10);
            meetupRepository.save(meetup);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @When("I update the meetup {string} with title {string}")
    public void i_update_the_meetup_with_title(String title, String newTitle) throws Throwable {
        Meetup meetup = meetupRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst().orElseThrow();
        
        Map<String, Object> body = new HashMap<>();
        body.put("title", newTitle);

        stepDefs.result = stepDefs.mockMvc.perform(patch("/meetups/" + meetup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(body))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(meetup.getOrganizer().getId()).roles("USER")));
    }

    @Then("the meetup {string} now has title {string}")
    public void the_meetup_now_has_title(String title, String newTitle) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + newTitle + "')].title").value(newTitle));
    }

    @When("I delete the meetup {string}")
    public void i_delete_the_meetup(String title) throws Throwable {
        Meetup meetup = meetupRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst().orElseThrow();

        stepDefs.result = stepDefs.mockMvc.perform(delete("/meetups/" + meetup.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(meetup.getOrganizer().getId()).roles("USER")));
    }

    @Then("the meetup {string} no longer exists")
    public void the_meetup_no_longer_exists(String title) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')]", org.hamcrest.Matchers.empty()));
    }

    @Then("the meetup {string} has location {string} in {string}")
    public void the_meetup_has_location_in(String title, String locationName, String city) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].location.name").value(locationName))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].location.city").value(city));
    }

    @When("I create a meetup with title {string}")
    public void i_create_a_meetup_with_title(String title) throws Throwable {
        User organizer = userRepository.findById("organizer").orElseGet(() -> {
            User u = new User();
            u.setId("organizer");
            u.setPassword(User.passwordEncoder.encode("password"));
            u.setEmail("organizer@example.com");
            return userRepository.save(u);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("startDate", Instant.now().plusSeconds(3600));
        body.put("endDate", Instant.now().plusSeconds(7200));
        body.put("capacity", 10);

        stepDefs.result = stepDefs.mockMvc.perform(post("/meetups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(body))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("organizer").roles("USER")));
    }

    @Then("the meetup {string} should have status {string}")
    public void the_meetup_should_have_status(String title, String status) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].status").value(status));
    }

    @Given("a meetup {string} exists")
    public void a_meetup_exists(String title) {
        a_meetup_exists_organized_by(title, "organizer");
    }

    @Given("a meetup {string} exists created by {string}")
    public void a_meetup_exists_created_by(String title, String username) {
        a_meetup_exists_organized_by(title, username);
    }

    @Given("a meetup {string} exists with status {string}")
    public void a_meetup_exists_with_status(String title, String status) {
        User organizer = userRepository.findById("organizer").orElseGet(() -> {
            User u = new User();
            u.setId("organizer");
            u.setPassword(User.passwordEncoder.encode("password"));
            u.setEmail("organizer@example.com");
            return userRepository.save(u);
        });

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "organizer", null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
        );
        try {
            Meetup meetup = new Meetup();
            meetup.setTitle(title);
            meetup.setOrganizer(organizer);
            meetup.setStartDate(Instant.now().plusSeconds(3600));
            meetup.setEndDate(Instant.now().plusSeconds(7200));
            meetup.setCapacity(10);
            meetup.setStatus(MeetupStatus.valueOf(status));
            meetupRepository.save(meetup);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @When("I update the meetup {string} status to {string}")
    public void i_update_the_meetup_status_to(String title, String status) throws Throwable {
        Meetup meetup = meetupRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst().orElseThrow();

        Map<String, Object> body = new HashMap<>();
        body.put("status", status);

        stepDefs.result = stepDefs.mockMvc.perform(patch("/meetups/" + meetup.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(stepDefs.mapper.writeValueAsString(body))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(stepDefs.currentUser).roles("USER")));
    }
}
