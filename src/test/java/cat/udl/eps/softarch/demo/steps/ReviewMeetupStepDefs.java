package cat.udl.eps.softarch.demo.steps;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.Review;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.ReviewRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ReviewMeetupStepDefs {

    private final StepDefs stepDefs;
    private final UserRepository userRepository;
    private final MeetupRepository meetupRepository;
    private final ParticipationRepository participationRepository;
    private final ReviewRepository reviewRepository;

    public ReviewMeetupStepDefs(StepDefs stepDefs, UserRepository userRepository,
            MeetupRepository meetupRepository, ParticipationRepository participationRepository,
            ReviewRepository reviewRepository) {
        this.stepDefs = stepDefs;
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.participationRepository = participationRepository;
        this.reviewRepository = reviewRepository;
    }

    @Given("There is a {string} meetup {string} with a confirmed participant {string}")
    @Given("There is an {string} meetup {string} with a confirmed participant {string}")
    public void thereIsAMeetupWithConfirmedParticipant(String status, String title, String username) {
        User participant = userRepository.findById(username).orElseThrow();

        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(10);
        meetup.setVisibility(Meetup.Visibility.PUBLIC);
        meetup.setStatus(Meetup.MeetupStatus.valueOf(status));
        meetup.setOrganizer(participant);
        Meetup saved = RunAs.admin(() -> meetupRepository.save(meetup));

        Participation participation = new Participation();
        participation.setParticipant(participant);
        participation.setMeetup(saved);
        participation.setStatus(ParticipationStatus.CONFIRMED);
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);
    }

    @Given("There is a {string} meetup {string} organized by {string} with a confirmed participant {string}")
    public void thereIsAMeetupOrganizedByWithConfirmedParticipant(String status, String title,
            String organizerId, String participantId) {
        User organizer = userRepository.findById(organizerId).orElseThrow();
        User participant = userRepository.findById(participantId).orElseThrow();

        Meetup meetup = new Meetup();
        meetup.setTitle(title);
        meetup.setCapacity(10);
        meetup.setVisibility(Meetup.Visibility.PUBLIC);
        meetup.setStatus(Meetup.MeetupStatus.valueOf(status));
        meetup.setOrganizer(organizer);
        Meetup saved = RunAs.admin(() -> meetupRepository.save(meetup));

        Participation participation = new Participation();
        participation.setParticipant(participant);
        participation.setMeetup(saved);
        participation.setStatus(ParticipationStatus.CONFIRMED);
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);
    }

    @Given("{string} has already reviewed the meetup {string} with rating {int}")
    public void hasAlreadyReviewedTheMeetup(String username, String title, int rating) {
        User reviewer = userRepository.findById(username).orElseThrow();
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();

        Review review = new Review();
        review.setRating(rating);
        review.setText("Existing review");
        review.setReviewer(reviewer);
        review.setMeetup(meetup);
        RunAs.admin(() -> reviewRepository.save(review));
    }

    @When("I review the meetup {string} with rating {int} and text {string}")
    public void iReviewTheMeetup(String title, int rating, String text) throws Throwable {
        Meetup meetup = meetupRepository.findByTitle(title).orElseThrow();

        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("text", text);
        body.put("meetup", meetup.getUri());

        stepDefs.result = stepDefs.mockMvc.perform(
                post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stepDefs.mapper.writeValueAsString(body))
                    .characterEncoding(StandardCharsets.UTF_8)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(AuthenticationStepDefs.authenticate()))
            .andDo(print());
    }

    @When("I request the average rating of organizer {string}")
    public void iRequestTheAverageRatingOfOrganizer(String organizerId) throws Throwable {
        stepDefs.result = stepDefs.mockMvc.perform(
                get("/reviews/search/findAverageRatingByOrganizer")
                    .param("organizerId", organizerId)
                    .accept(MediaType.APPLICATION_JSON))
            .andDo(print());
    }

    @Then("The average rating is {double}")
    public void theAverageRatingIs(double average) throws Throwable {
        stepDefs.result.andExpect(jsonPath("$.average").value(average));
    }

    @Then("The average rating is null")
    public void theAverageRatingIsNull() throws Throwable {
        stepDefs.result.andExpect(jsonPath("$.average").value(nullValue()));
    }
}
