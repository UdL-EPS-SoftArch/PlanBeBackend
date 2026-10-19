package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.NotificationType;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Waiting list (#22) and #44 SPOT_FREED. The only place where the system moves a participation from WAITING
 * to CONFIRMED, whatever freed the spot: a confirmed participant cancelling or deleting their participation,
 * the organizer raising the capacity or (re)opening the meetup.
 */
@Component
public class WaitingList {

    private static final Logger log = LoggerFactory.getLogger(WaitingList.class);
    private static final Set<MeetupStatus> CLOSED = EnumSet.of(MeetupStatus.CANCELLED, MeetupStatus.FINISHED);

    @PersistenceContext
    private EntityManager entityManager;

    private final ParticipationRepository participationRepository;
    private final Notifier notifier;

    public WaitingList(ParticipationRepository participationRepository, Notifier notifier) {
        this.participationRepository = participationRepository;
        this.notifier = notifier;
    }

    /**
     * Gives every free spot to the oldest WAITING participants (joinedAt, then id), notifies each of them
     * with SPOT_FREED and, for OPEN/FULL meetups, sets OPEN or FULL from the confirmed count.
     * CANCELLED and FINISHED meetups promote nobody.
     */
    @Transactional
    public void fillFreeSpots(Long meetupId) {
        fillFreeSpots(meetupId, null);
    }

    /**
     * Same as {@link #fillFreeSpots(Long)}, but {@code joiningUsername} (who has just joined and gets a spot
     * straight away) is not sent SPOT_FREED: nothing was "freed" for them.
     */
    @Transactional
    public void fillFreeSpots(Long meetupId, String joiningUsername) {
        Meetup meetup = lockFresh(meetupId);
        if (CLOSED.contains(meetup.getStatus())) return;
        if (meetup.getCapacity() == null) throw new IllegalStateException("Meetup " + meetupId + " has no capacity");

        long confirmed = participationRepository.countByMeetupIdAndStatus(meetupId, ParticipationStatus.CONFIRMED);
        long freeSpots = meetup.getCapacity() - confirmed;
        if (freeSpots > 0) {
            List<Participation> queue = participationRepository
                .findByMeetupIdAndStatusOrderByJoinedAtAscIdAsc(meetupId, ParticipationStatus.WAITING);
            for (Participation promoted : queue.subList(0, (int) Math.min(freeSpots, queue.size()))) {
                promoted.setStatus(ParticipationStatus.CONFIRMED);
                participationRepository.save(promoted);
                if (!promoted.getParticipant().getId().equals(joiningUsername)) {
                    notifier.notify(promoted.getParticipant(), NotificationType.SPOT_FREED,
                        "A spot has been freed in the meetup \"%s\": your participation is now confirmed"
                            .formatted(meetup.getTitle()),
                        meetup);
                }
                confirmed++;
            }
        }
        if (confirmed > meetup.getCapacity()) {
            // Only possible by hand (an ADMIN confirming someone, or a capacity lowered below the confirmed
            // participants). Nobody is demoted silently: the meetup stays FULL and it is logged.
            log.warn("Meetup {} has {} confirmed participants for a capacity of {}", meetupId, confirmed,
                meetup.getCapacity());
        }
        if (meetup.getStatus() == MeetupStatus.OPEN || meetup.getStatus() == MeetupStatus.FULL) {
            // Managed, fresh and locked instance; @DynamicUpdate writes only the status column.
            meetup.setStatus(confirmed >= meetup.getCapacity() ? MeetupStatus.FULL : MeetupStatus.OPEN);
        }
    }

    /**
     * Locks the meetup row (SELECT ... FOR UPDATE) and reloads its state from the database.
     * A plain locking query is not enough: with open-in-view the request's persistence context usually
     * holds the meetup already (Spring Data REST loaded it to resolve "meetup": "/meetups/1", or through the
     * participation), and a query returns that cached instance without refreshing it. Deciding on that
     * stale copy could promote people in a meetup cancelled a moment ago, or write old values back.
     * refresh() takes the lock and re-reads the row, so every decision below uses the committed state.
     */
    private Meetup lockFresh(Long meetupId) {
        Meetup meetup = entityManager.find(Meetup.class, meetupId);
        if (meetup == null) throw new IllegalStateException("Meetup " + meetupId + " does not exist");
        entityManager.refresh(meetup, LockModeType.PESSIMISTIC_WRITE);
        return meetup;
    }
}
