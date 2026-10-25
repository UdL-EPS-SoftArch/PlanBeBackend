package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Interest;
import cat.udl.eps.softarch.demo.domain.Location;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import cat.udl.eps.softarch.demo.repository.InterestRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class RecommendMeetupsStepDefs {
    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final InterestRepository interestRepository;
    private final TransactionTemplate transactionTemplate;

    public RecommendMeetupsStepDefs(StepDefs stepDefs, UserRepository userRepository,
        MeetupRepository meetupRepository, InterestRepository interestRepository,
        TransactionTemplate transactionTemplate) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.interestRepository = interestRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Given("User {string} lives in {string}")
    public void userLivesIn(String username, String city) {
        setCity(username, city);
    }

    @Given("User {string} has no city")
    public void userHasNoCity(String username) {
        setCity(username, null);
    }

    @Given("The interest {string} belongs to the category {string}")
    public void theInterestBelongsToTheCategory(String name, String category) {
        Interest interest = interestRepository.findByName(name).orElseThrow();
        interest.setCategory(Category.valueOf(category));
        interestRepository.save(interest);
    }

    @Given("There are the following meetups organized by {string}:")
    public void thereAreTheFollowingMeetupsOrganizedBy(String organizerUsername, List<Map<String, String>> rows) {
        User organizer = userRepository.findById(organizerUsername).orElseThrow();
        for (Map<String, String> row : rows) {
            Instant start = Instant.now().plus(Duration.ofDays(Long.parseLong(row.get("startsInDays"))));
            Meetup meetup = new Meetup();
            meetup.setTitle(row.get("title"));
            meetup.setCapacity(10);
            meetup.setStartDate(start);
            meetup.setEndDate(start.plus(Duration.ofHours(2)));
            meetup.setLocation(new Location(null, null, row.get("city"), null, null));
            meetup.setStatus(MeetupStatus.valueOf(row.getOrDefault("status", "OPEN")));
            meetup.setVisibility(Visibility.valueOf(row.getOrDefault("visibility", "PUBLIC")));
            meetup.setOrganizer(organizer);
            // Empty cells come as null: no tags / no category
            if (row.get("tags") != null) {
                for (String tag : row.get("tags").split(",")) {
                    meetup.getTags().add(interestRepository.findByName(tag.trim()).orElseThrow());
                }
            }
            if (row.get("category") != null) {
                meetup.setCategory(Category.valueOf(row.get("category")));
            }
            RunAs.admin(() -> meetupRepository.save(meetup));
        }
    }

    @Given("I create a meetup {string} in {string} tagged with {string}")
    public void iCreateAMeetupInTaggedWith(String title, String city, String tag) throws Throwable {
        Interest interest = interestRepository.findByName(tag).orElseThrow();
        Instant start = Instant.now().plus(Duration.ofDays(3));
        JSONObject meetup = new JSONObject();
        meetup.put("title", title);
        meetup.put("capacity", 10);
        meetup.put("startDate", start.toString());
        meetup.put("endDate", start.plus(Duration.ofHours(2)).toString());
        meetup.put("location", new JSONObject().put("city", city));
        meetup.put("tags", new JSONArray().put(interest.getUri()));

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/meetups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(meetup.toString())
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I request my recommended meetups")
    public void iRequestMyRecommendedMeetups() throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/meetups/search/recommended")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The recommended meetups are {string}")
    public void theRecommendedMeetupsAre(String titles) throws Throwable {
        String[] expected = Arrays.stream(titles.split(",")).map(String::trim).toArray(String[]::new);
        stepDefs.result.andExpect(jsonPath("$._embedded.meetups[*].title", contains(expected)));
    }

    @And("There are no recommended meetups")
    public void thereAreNoRecommendedMeetups() throws Throwable {
        stepDefs.result.andExpect(jsonPath("$._embedded.meetups[*].title", empty()));
    }

    private void setCity(String username, String city) {
        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(username).orElseThrow();
            user.setCity(city);
            userRepository.save(user);
        });
    }
}
