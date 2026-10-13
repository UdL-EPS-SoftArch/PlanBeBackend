package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;

@Component
@RepositoryEventHandler
public class ParticipationEventHandler {

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private UserRepository userRepository;

    @HandleBeforeCreate
    public void handleBeforeCreate(Participation participation) {
        if (participation.getMeetup() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is required");
        }

        Meetup meetup = participation.getMeetup();

        // Assign current authenticated user as participant
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findById(currentUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        participation.setParticipant(user);

        // Handle visibility: if meetup is PRIVATE, throw 403 Forbidden
        if (meetup.getVisibility() == Meetup.Visibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot join private meetup without invitation");
        }

        // Prevent duplicate joins
        Optional<Participation> existing = participationRepository.findByParticipantIdAndMeetupId(
            user.getId(),
            meetup.getId()
        );
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined this meetup");
        }

        // Handle capacity checks: if meetup.getCapacity() is reached, set status to WAITING instead of CONFIRMED
        long currentParticipants = participationRepository.countByMeetupId(meetup.getId());
        if (currentParticipants >= meetup.getCapacity()) {
            participation.setStatus(ParticipationStatus.WAITING);
        } else {
            participation.setStatus(ParticipationStatus.CONFIRMED);
        }

        participation.setJoinedAt(Instant.now());
    }
}
