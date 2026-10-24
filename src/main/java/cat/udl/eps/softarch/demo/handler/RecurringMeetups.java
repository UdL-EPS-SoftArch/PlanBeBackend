package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Location;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.Recurrence;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;

/**
 * Creates the next occurrence of a recurring meetup once the current one is FINISHED (#53).
 */
@Component
public class RecurringMeetups {

    private final MeetupRepository meetupRepository;

    public RecurringMeetups(MeetupRepository meetupRepository) {
        this.meetupRepository = meetupRepository;
    }

    /**
     * The next occurrence is an OPEN copy of the meetup (without participants or invitations) at the first date
     * of the series that is still in the future, so a meetup finished late does not create one already past.
     * Nothing is created if the meetup does not repeat, that date goes beyond recurrenceEnd (inclusive, UTC
     * day) or the next occurrence already exists (the meetup was reopened and finished again).
     */
    public void createNextOccurrence(Meetup finished) {
        Recurrence recurrence = finished.getRecurrence();
        if (recurrence == null || recurrence == Recurrence.NONE || finished.getStartDate() == null) return;
        if (!meetupRepository.findByPreviousOccurrenceId(finished.getId()).isEmpty()) return;

        Instant now = Instant.now();
        Instant nextStart;
        int step = 1;
        do {
            nextStart = shift(finished.getStartDate(), recurrence, step++);
        } while (!nextStart.isAfter(now));

        LocalDate end = finished.getRecurrenceEnd();
        if (end != null && LocalDate.ofInstant(nextStart, ZoneOffset.UTC).isAfter(end)) return;

        Meetup next = new Meetup();
        next.setTitle(finished.getTitle());
        next.setDescription(finished.getDescription());
        next.setCapacity(finished.getCapacity());
        next.setCategory(finished.getCategory());
        next.setVisibility(finished.getVisibility());
        next.setStatus(MeetupStatus.OPEN);
        next.setOrganizer(finished.getOrganizer());
        next.setMinAge(finished.getMinAge());
        next.setMaxAge(finished.getMaxAge());
        next.setTargetGender(finished.getTargetGender());
        next.setPrice(finished.getPrice());
        next.setTags(new HashSet<>(finished.getTags()));
        next.setRecurrence(recurrence);
        next.setRecurrenceEnd(end);
        next.setPreviousOccurrence(finished);
        next.setStartDate(nextStart);
        if (finished.getEndDate() != null) {
            next.setEndDate(nextStart.plus(Duration.between(finished.getStartDate(), finished.getEndDate())));
        }
        Location location = finished.getLocation();
        if (location != null) {
            next.setLocation(new Location(location.getName(), location.getAddress(), location.getCity(),
                location.getLatitude(), location.getLongitude()));
        }
        meetupRepository.save(next);
    }

    /** MONTHLY adds calendar months, clamping to the last day of shorter months (Jan 31 -> Feb 28). */
    private static Instant shift(Instant start, Recurrence recurrence, int steps) {
        return switch (recurrence) {
            case WEEKLY -> start.plus(Duration.ofDays(7L * steps));
            case BIWEEKLY -> start.plus(Duration.ofDays(14L * steps));
            case MONTHLY -> start.atZone(ZoneOffset.UTC).plusMonths(steps).toInstant();
            case NONE -> start;
        };
    }
}
