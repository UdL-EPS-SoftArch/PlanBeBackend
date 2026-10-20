package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.NotificationType;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * Join / leave a meetup and waiting list (#22). Promotions and SPOT_FREED (#44) are done by {@link WaitingList}.
 *
 * Through the API a participant can only cancel their own participation (PATCH status=CANCELLED) or delete it:
 * setting oneself CONFIRMED or moving joinedAt would skip the queue and the SPOT_FREED notice.
 * To come back after cancelling, the user joins again with POST /participations and goes to the end of the queue.
 * An ADMIN may also confirm a WAITING participant by hand. Nobody, an ADMIN included, may move a participation
 * to WAITING or reactivate a CANCELLED one (see handleBeforeSave).
 */
@Component
@RepositoryEventHandler
public class ParticipationEventHandler {

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final WaitingList waitingList;
    private final Notifier notifier;

    public ParticipationEventHandler(ParticipationRepository participationRepository, UserRepository userRepository,
                                     WaitingList waitingList, Notifier notifier) {
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
        this.waitingList = waitingList;
        this.notifier = notifier;
    }

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

        // Check age and gender restrictions
        if (meetup.getMinAge() != null) {
            Integer age = user.getAge();
            if (age == null || age < meetup.getMinAge()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not meet age requirements");
            }
        }
        if (meetup.getMaxAge() != null) {
            Integer age = user.getAge();
            if (age == null || age > meetup.getMaxAge()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not meet age requirements");
            }
        }
        if (meetup.getTargetGender() != null) {
            if (user.getGender() == null || user.getGender() != meetup.getTargetGender()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not meet gender requirements");
            }
        }

        // Prevent duplicate joins
        Optional<Participation> existing = participationRepository.findByParticipantIdAndMeetupId(
            user.getId(),
            meetup.getId()
        );
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined this meetup");
        if (meetup.getStatus() == MeetupStatus.CANCELLED || meetup.getStatus() == MeetupStatus.FINISHED) {
            // Nobody would ever promote or notify this participation: fail clearly instead of a silent dead end.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot join a meetup that is " + meetup.getStatus());
        }

        // Prevent duplicate joins. A CANCELLED participation does not count: the user may join again,
        // and the old row is kept untouched as history (nothing is deleted before the new row is saved).
        if (participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(user.getId(), meetup.getId(),
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined this meetup");
        }

        // Every newcomer enters the queue. Whether they get a spot is decided right after the insert by
        // WaitingList, under a lock on the meetup row: two simultaneous joins for the last spot cannot both
        // be confirmed, and nobody skips people who were already waiting (joining order is kept).
        participation.setStatus(ParticipationStatus.WAITING);
        participation.setJoinedAt(Instant.now());
    }

    @HandleAfterCreate
    public void handleAfterCreate(Participation participation) {
        // The joining user is not sent SPOT_FREED for the spot they get when joining.
        waitingList.fillFreeSpots(participation.getMeetup().getId(), participation.getParticipant().getId());
        // Answer with the status stored in the database (a scalar query, never a cached instance).
        participation.setStatus(participationRepository.findStatusById(participation.getId())
            .orElseThrow(() -> new IllegalStateException("Participation " + participation.getId() + " vanished")));
    }

    @HandleBeforeSave
    public void handleBeforeSave(Participation participation) {
        Participation.LoadedState before = checkCanModify(participation);
        if (participation.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The status of a participation is required");
        }

        boolean sameIdentity = Objects.equals(before.participantId(),
                participation.getParticipant() == null ? null : participation.getParticipant().getId())
            && Objects.equals(before.meetupId(), participation.getMeetup() == null ? null : participation.getMeetup().getId())
            && Objects.equals(before.joinedAt(), participation.getJoinedAt());
        if (!sameIdentity) {
            // e.g. jumping the queue by changing joinedAt
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the status of a participation can be changed");
        }
        ParticipationStatus from = before.status();
        ParticipationStatus to = participation.getStatus();
        if (from == to) return;
        // Rules for everyone, an ADMIN included:
        if (from == ParticipationStatus.CANCELLED) {
            // Reactivating would bypass the queue (and could clash with a newer active participation).
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A cancelled participation cannot be reactivated: join the meetup again with POST /participations");
        }
        if (to == ParticipationStatus.WAITING) {
            // Only the system puts someone on the waiting list. A demoted CONFIRMED participant keeps their old
            // joinedAt, so WaitingList would promote them straight back with a false SPOT_FREED and skip the
            // people really waiting.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "A participation cannot be moved to the waiting list by hand");
        }
        if (!isAdmin() && to != ParticipationStatus.CANCELLED) {
            // e.g. a WAITING user setting themselves to CONFIRMED: only WaitingList (or an ADMIN) promotes
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A participation can only be cancelled");
        }
        // Left: anyone may cancel (CONFIRMED/WAITING -> CANCELLED); an ADMIN may confirm a WAITING
        // participant (WAITING -> CONFIRMED), which sends SPOT_FREED in handleAfterSave.
    }

    @HandleBeforeLinkSave
    public void handleBeforeLinkSave(Participation participation, Object linked) {
        checkCanModify(participation);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The meetup or participant of a participation cannot be changed");
    }

    @HandleBeforeLinkDelete
    public void handleBeforeLinkDelete(Participation participation, Object linked) {
        checkCanModify(participation);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The meetup or participant of a participation cannot be changed");
    }

    @HandleBeforeDelete
    public void handleBeforeDelete(Participation participation) {
        checkCanModify(participation);
    }

    @HandleAfterSave
    public void handleAfterSave(Participation participation) {
        Participation.LoadedState before = requireLoadedState(participation);
        if (before.status() == participation.getStatus()) return;
        if (before.status() == ParticipationStatus.WAITING && participation.getStatus() == ParticipationStatus.CONFIRMED) {
            // #44: WAITING -> CONFIRMED by hand (only an ADMIN can do it) also tells the user.
            Meetup meetup = participation.getMeetup();
            notifier.notify(participation.getParticipant(), NotificationType.SPOT_FREED,
                "Your participation in the meetup \"%s\" is now confirmed".formatted(meetup.getTitle()),
                meetup);
        }
        // Frees the spot of a cancelled participant (SPOT_FREED to the next one) and keeps OPEN/FULL right.
        waitingList.fillFreeSpots(participation.getMeetup().getId());
        participation.captureLoadedState();
    }

    @HandleAfterDelete
    public void handleAfterDelete(Participation participation) {
        if (participation.getStatus() == ParticipationStatus.CONFIRMED) {
            waitingList.fillFreeSpots(participation.getMeetup().getId());
        }
    }

    private static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private Participation.LoadedState checkCanModify(Participation participation) {
        Participation.LoadedState before = requireLoadedState(participation);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin() && !authentication.getName().equals(before.participantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the participant can modify their participation");
        }
        return before;
    }

    private static Participation.LoadedState requireLoadedState(Participation participation) {
        Participation.LoadedState before = participation.loadedState();
        if (before == null) {
            // Never guess what changed: refuse loudly instead of silently skipping promotions.
            throw new IllegalStateException("Participation " + participation.getId() + " has no loaded state");
        }
        return before;
    }
}
