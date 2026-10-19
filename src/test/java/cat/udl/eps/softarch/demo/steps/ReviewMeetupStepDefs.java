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
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

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
        meetup = meetupRepository.save(meetup);

        Participation participation = new Participation();
        participation.setParticipant(participant);
        participation.setMeetup(meetup);
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
        reviewRepository.save(review);
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
}
