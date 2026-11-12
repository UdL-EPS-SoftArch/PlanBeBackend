package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.Review;
import cat.udl.eps.softarch.demo.domain.enums.Visibility;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.ReviewRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/** ApiSecurity.feature: #88 authentication, #89 participations, #90 reviews, #91 internal queries, #92 deletes. */
public class ApiSecurityStepDefs {

    @Autowired
    private StepDefs stepDefs;

    @Autowired
    private MeetupRepository meetupRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Given("The meetup {string} has visibility {string}")
    public void theMeetupHasVisibility(String title, String visibility) {
        Meetup meetup = meetup(title);
        meetup.setVisibility(Visibility.valueOf(visibility));
        RunAs.admin(() -> meetupRepository.save(meetup));
    }

    @When("I send an empty POST request to {string}")
    public void iSendAnEmptyPostRequestTo(String path) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I send a GET request to {string}")
    public void iSendAGetRequestTo(String path) throws Throwable {
        perform(path);
    }

    @When("I request the participant of the participation of {string} in the meetup {string}")
    public void iRequestTheParticipantOfTheParticipation(String username, String title) throws Throwable {
        perform("/participations/" + participation(username, title).getId() + "/participant");
    }

    @When("I list the participations")
    public void iListTheParticipations() throws Throwable {
        perform("/participations");
    }

    @When("I retrieve the participation of {string} in the meetup {string}")
    public void iRetrieveTheParticipation(String username, String title) throws Throwable {
        perform("/participations/" + participation(username, title).getId());
    }

    @When("I look up the participation of {string} in the meetup {string} through the search endpoint")
    public void iLookUpTheParticipationThroughTheSearchEndpoint(String username, String title) throws Throwable {
        perform("/participations/search/findByParticipantIdAndMeetupId?participantId=" + username
            + "&meetupId=" + meetup(title).getId());
    }

    @When("I list the reviews of the meetup {string}")
    public void iListTheReviewsOfTheMeetup(String title) throws Throwable {
        perform("/reviews/search/findByMeetupId?meetupId=" + meetup(title).getId());
    }

    @When("I retrieve the review of {string} on the meetup {string}")
    public void iRetrieveTheReview(String username, String title) throws Throwable {
        perform("/reviews/" + review(username, title).getId());
    }

    @When("I look up the review of {string} on the meetup {string} through the search endpoint")
    public void iLookUpTheReviewThroughTheSearchEndpoint(String username, String title) throws Throwable {
        perform("/reviews/search/findByReviewerIdAndMeetupId?reviewerId=" + username
            + "&meetupId=" + meetup(title).getId());
    }

    @When("I search the groups by name {string}")
    public void iSearchTheGroupsByName(String name) throws Throwable {
        perform("/groups/search/findByName?name=" + name);
    }

    @When("I try to delete the meetup {string}")
    public void iTryToDeleteTheMeetup(String title) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                delete("/meetups/" + meetup(title).getId())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @Then("The response contains {int} participation(s)")
    public void theResponseContainsParticipations(int count) throws Throwable {
        assertThat(embedded("participations").size(), is(count));
    }

    @And("The response contains {int} participation(s) of {string}")
    public void theResponseContainsParticipationsOf(int count, String username) throws Throwable {
        JsonNode participations = embedded("participations");
        assertThat(participations.size(), is(count));
        for (JsonNode participation : participations) {
            String href = participation.path("_links").path("participant").path("href").asString();
            String body = stepDefs.mockMvc.perform(get(href).with(AuthenticationStepDefs.authenticate()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(stepDefs.mapper.readTree(body).path("_links").path("self").path("href").asString(),
                endsWith("/users/" + username));
        }
    }

    @Then("The response contains {int} review(s)")
    public void theResponseContainsReviews(int count) throws Throwable {
        assertThat(embedded("reviews").size(), is(count));
    }

    @Then("The response contains {int} group(s)")
    public void theResponseContainsGroups(int count) throws Throwable {
        assertThat(embedded("groups").size(), is(count));
    }

    private void perform(String path) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    /** The embedded collection of a HAL response; an empty one may come without the _embedded key. */
    private JsonNode embedded(String collection) throws Throwable {
        String body = stepDefs.result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return stepDefs.mapper.readTree(body).path("_embedded").path(collection);
    }

    private Meetup meetup(String title) {
        return meetupRepository.findByTitle(title).orElseThrow();
    }

    private Participation participation(String username, String title) {
        return participationRepository.findLatestByParticipantIdAndMeetupId(username, meetup(title).getId())
            .orElseThrow();
    }

    private Review review(String username, String title) {
        return reviewRepository.findAnyByReviewerIdAndMeetupId(username, meetup(title).getId()).orElseThrow();
    }
}
