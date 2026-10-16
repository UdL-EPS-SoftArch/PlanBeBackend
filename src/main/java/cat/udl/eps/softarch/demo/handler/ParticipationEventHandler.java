package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
public class ParticipationEventHandler {

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetupRepository meetupRepository;

    @HandleBeforeCreate
    public void handleBeforeCreate(Participation participation) {
        if (participation.getMeetup() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is required");
        }

        Meetup meetup = participation.getMeetup();

        // Cancelled meetups do not accept new participations
        if (meetup.getStatus() == Meetup.MeetupStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot join a cancelled meetup");
        }

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

        // Handle capacity checks: if capacity is reached, put the user on the waiting list
        if (countConfirmed(meetup.getId()) >= meetup.getCapacity()) {
            participation.setStatus(ParticipationStatus.WAITING);
        } else {
            participation.setStatus(ParticipationStatus.CONFIRMED);
        }

        participation.setJoinedAt(Instant.now());
    }

    @HandleAfterCreate
    @Transactional
    public void handleAfterCreate(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) {
            return;
        }
        if (countConfirmed(meetup.getId()) >= meetup.getCapacity()) {
            meetup.setStatus(Meetup.MeetupStatus.FULL);
            meetupRepository.save(meetup);
        }
    }

    @HandleAfterDelete
    @Transactional
    public void handleAfterDelete(Participation participation) {
        Meetup meetup = participation.getMeetup();
        if (meetup == null) {
            return;
        }

        // If a confirmed user leaves, promote the first waiting user (if any)
        if (participation.getStatus() == ParticipationStatus.CONFIRMED) {
            Participation nextInLine = StreamSupport
                .stream(participationRepository.findAll().spliterator(), false)
                .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(meetup.getId()))
                .filter(p -> p.getStatus() == ParticipationStatus.WAITING)
                .min((p1, p2) -> p1.getId().compareTo(p2.getId()))
                .orElse(null);

            if (nextInLine != null) {
                nextInLine.setStatus(ParticipationStatus.CONFIRMED);
                participationRepository.save(nextInLine);

                // The spot was immediately filled, so the meetup stays FULL
                meetup.setStatus(Meetup.MeetupStatus.FULL);
                meetupRepository.save(meetup);
                return;
            }
        }

        if (countConfirmed(meetup.getId()) < meetup.getCapacity()) {
            meetup.setStatus(Meetup.MeetupStatus.OPEN);
            meetupRepository.save(meetup);
        }
    }

    private long countConfirmed(Long meetupId) {
        return StreamSupport.stream(participationRepository.findAll().spliterator(), false)
            .filter(p -> p.getMeetup() != null && p.getMeetup().getId().equals(meetupId))
            .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
            .count();
    }
}
