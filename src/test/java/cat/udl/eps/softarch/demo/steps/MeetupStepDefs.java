package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Location;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.time.Instant;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.MeetupVisibility;

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
        else body.put("startDate", Instant.now().plusSeconds(3600).toString());
        if (details.containsKey("endDate")) body.put("endDate", details.get("endDate"));
        else body.put("endDate", Instant.now().plusSeconds(7200).toString());
        if (details.containsKey("minAge")) body.put("minAge", Integer.parseInt(details.get("minAge")));
        if (details.containsKey("maxAge")) body.put("maxAge", Integer.parseInt(details.get("maxAge")));
        if (details.containsKey("targetGender")) body.put("targetGender", details.get("targetGender"));
        if (details.containsKey("price")) body.put("price", new java.math.BigDecimal(details.get("price")));
        
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

    @Given("the following meetups exist:")
    public void the_following_meetups_exist(io.cucumber.datatable.DataTable dataTable) {
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
            List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
            for (Map<String, String> row : rows) {
                String organizerId = row.getOrDefault("organizer", "organizer");
                User rowOrganizer = userRepository.findById(organizerId).orElseGet(() -> {
                    User u = new User();
                    u.setId(organizerId);
                    u.setPassword(User.passwordEncoder.encode("password"));
                    u.setEmail(organizerId + "@example.com");
                    return userRepository.save(u);
                });

                Meetup meetup = new Meetup();
                meetup.setTitle(row.get("title"));
                meetup.setOrganizer(rowOrganizer);
                meetup.setStartDate(row.containsKey("startDate") ? Instant.parse(row.get("startDate")) : Instant.now().plusSeconds(3600));
                meetup.setEndDate(row.containsKey("endDate") ? Instant.parse(row.get("endDate")) : Instant.now().plusSeconds(7200));
                meetup.setCapacity(row.containsKey("capacity") ? Integer.parseInt(row.get("capacity")) : 10);
                if (row.containsKey("category")) meetup.setCategory(cat.udl.eps.softarch.demo.domain.enums.Category.valueOf(row.get("category")));
                if (row.containsKey("visibility")) meetup.setVisibility(MeetupVisibility.valueOf(row.get("visibility")));
                if (row.containsKey("status")) meetup.setStatus(MeetupStatus.valueOf(row.get("status")));
                if (row.containsKey("city")) {
                    Location loc = new Location();
                    loc.setCity(row.get("city"));
                    meetup.setLocation(loc);
                }
                meetupRepository.save(meetup);
            }
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @When("I search for meetups by category {string}")
    public void i_search_for_meetups_by_category(String category) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/search/findByCategoryAndVisibility")
                .param("category", category));
    }

    @When("I search for meetups by city {string}")
    public void i_search_for_meetups_by_city(String city) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/search/findByCityAndVisibility")
                .param("city", city));
    }

    @When("I search for meetups after {string}")
    public void i_search_for_meetups_after(String date) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/search/findByStartDateAfterAndVisibility")
                .param("startDate", date));
    }

    @When("I search for meetups by organizer {string}")
    public void i_search_for_meetups_by_organizer(String organizer) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/search/findByOrganizerAndVisibility")
                .param("organizerId", organizer));
    }

    @Then("the meetup {string} should have status {string}")
    public void the_meetup_should_have_status(String title, String status) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].status").value(status));
    }

    @Then("the meetup {string} has minAge {int} and maxAge {int} and targetGender {string}")
    public void the_meetup_has_age_range_and_gender(String title, Integer minAge, Integer maxAge, String gender) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].minAge").value(minAge))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].maxAge").value(maxAge))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].targetGender").value(gender));
    }

    @Then("the meetup {string} has price {double}")
    public void the_meetup_has_price(String title, Double price) throws Throwable {
        stepDefs.mockMvc.perform(get("/meetups"))
                .andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].price").value(price));
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

    @Given("a meetup {string} exists with visibility {string}")
    public void a_meetup_exists_with_visibility(String title, String visibility) {
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
            meetup.setVisibility(MeetupVisibility.valueOf(visibility));
            meetupRepository.save(meetup);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Given("a meetup {string} exists created by {string} with visibility {string}")
    public void a_meetup_exists_created_by_with_visibility(String title, String username, String visibility) {
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
            meetup.setVisibility(MeetupVisibility.valueOf(visibility));
            meetupRepository.save(meetup);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @When("I list meetups")
    public void i_list_meetups() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/search/findVisibleMeetups"));
    }

    @Then("I should see {string}")
    public void i_should_see(String title) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')].title").exists());
    }

    @Then("I should not see {string}")
    public void i_should_not_see(String title) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.meetups[?(@.title == '" + title + "')]", org.hamcrest.Matchers.empty()));
    }

    @When("I try to retrieve the meetup {string}")
    public void i_try_to_retrieve_the_meetup(String title) throws Throwable {
        Meetup meetup = meetupRepository.findAll().stream()
                .filter(m -> m.getTitle().equals(title))
                .findFirst().orElseThrow();

        stepDefs.result = stepDefs.mockMvc.perform(get("/meetups/" + meetup.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(stepDefs.currentUser).roles("USER")));
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
