package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    /**
     * The current (latest) participation of a user in a meetup. A user who left and joined again has an
     * older CANCELLED row kept as history, so "the" participation is the one with the highest id.
     */
    @Query("select p from Participation p where p.participant.id = ?1 and p.meetup.id = ?2 and p.id = "
        + "(select max(p2.id) from Participation p2 where p2.participant.id = ?1 and p2.meetup.id = ?2)")
    Optional<Participation> findByParticipantIdAndMeetupId(String participantId, Long meetupId);

    /**
     * Find participations for the current authenticated user.
     * Row-level authorization via SpEL.
     */
    @Query("select p from Participation p where p.participant.id = ?#{authentication.name} or ?#{hasRole('ADMIN')} = true")
    Iterable<Participation> findMyParticipations();

    // ---- Internal use only (capacity, waiting list and notifications) ----

    @RestResource(exported = false)
    long countByMeetupIdAndStatus(Long meetupId, ParticipationStatus status);

    @RestResource(exported = false)
    List<Participation> findByMeetupIdAndStatusIn(Long meetupId, Collection<ParticipationStatus> statuses);

    @RestResource(exported = false)
    List<Participation> findByMeetupIdAndStatusOrderByJoinedAtAscIdAsc(Long meetupId, ParticipationStatus status);

    @RestResource(exported = false)
    boolean existsByParticipantIdAndMeetupIdAndStatusIn(String participantId, Long meetupId,
                                                        Collection<ParticipationStatus> statuses);

    @RestResource(exported = false)
    List<Participation> findByParticipantIdAndMeetupIdOrderByIdAsc(String participantId, Long meetupId);

    // Used by BlockEventHandler (#48): participations of a user in the meetups of a given organizer
    @RestResource(exported = false)
    List<Participation> findByParticipantIdAndMeetupOrganizerIdAndStatusInAndMeetupStatusIn(String participantId,
        String organizerId, Collection<ParticipationStatus> statuses, Collection<MeetupStatus> meetupStatuses);

    @RestResource(exported = false)
    @Query("select p.status from Participation p where p.id = ?1")
    Optional<ParticipationStatus> findStatusById(Long id);
}
