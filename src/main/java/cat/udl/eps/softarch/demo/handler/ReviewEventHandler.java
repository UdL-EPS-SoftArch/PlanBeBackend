package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Review;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.ReviewRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RepositoryEventHandler
public class ReviewEventHandler {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @HandleBeforeCreate
    public void handleBeforeCreate(Review review) {
        Meetup meetup = review.getMeetup();
        if (meetup == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is required");
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reviewer = userRepository.findById(currentUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        review.setReviewer(reviewer);

        if (meetup.getStatus() != MeetupStatus.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot review a meetup that is not finished");
        }

        boolean confirmedParticipant = participationRepository
            .findByParticipantIdAndMeetupId(reviewer.getId(), meetup.getId())
            .map(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
            .orElse(false);
        if (!confirmedParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only confirmed participants can review this meetup");
        }

        if (reviewRepository.findByReviewerIdAndMeetupId(reviewer.getId(), meetup.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "User already reviewed this meetup");
        }
    }
}
