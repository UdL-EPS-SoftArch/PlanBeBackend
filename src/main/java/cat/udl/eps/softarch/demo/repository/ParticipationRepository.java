package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    // A participation is visible to its participant, the organizer of the meetup, the owner and the active
    // admins of the meetup's group, and the platform admins (#89).
    String VISIBILITY_CLAUSE =
        "p.participant.id = ?#{authentication.name} " +
        "or p.meetup.organizer.id = ?#{authentication.name} " +
        "or ?#{hasRole('ADMIN')} = true " +
        "or exists (select g from Group g where g.id = p.meetup.group.id and g.ownedBy.id = ?#{authentication.name}) " +
        "or exists (select ms from Membership ms where ms.group.id = p.meetup.group.id " +
        "and ms.member.id = ?#{authentication.name} " +
        "and ms.status.id = 'ACTIVE' " +
        "and ms.role in (cat.udl.eps.softarch.demo.domain.enums.GroupRole.OWNER, " +
        "cat.udl.eps.softarch.demo.domain.enums.GroupRole.ADMIN))";

    // The current (latest) participation of a user in a meetup. A user who left and joined again has an
    // older CANCELLED row kept as history, so "the" participation is the one with the highest id.
    String LATEST_CLAUSE = "p.participant.id = :participantId and p.meetup.id = :meetupId and p.id = "
        + "(select max(p2.id) from Participation p2 where p2.participant.id = :participantId "
        + "and p2.meetup.id = :meetupId)";

    @Override
    @Query("select p from Participation p where " + VISIBILITY_CLAUSE)
    Page<Participation> findAll(Pageable pageable);

    @Override
    @Query("select p from Participation p where p.id = :id and (" + VISIBILITY_CLAUSE + ")")
    Optional<Participation> findById(@Param("id") Long id);

    @Query("select p from Participation p where " + LATEST_CLAUSE + " and (" + VISIBILITY_CLAUSE + ")")
    Optional<Participation> findByParticipantIdAndMeetupId(@Param("participantId") String participantId,
                                                           @Param("meetupId") Long meetupId);

    /**
     * Find participations for the current authenticated user.
     * Row-level authorization via SpEL.
     */
    @Query("select p from Participation p where p.participant.id = ?#{authentication.name} or ?#{hasRole('ADMIN')} = true")
    Iterable<Participation> findMyParticipations();

    // ---- Internal use only (capacity, waiting list and notifications) ----

    @RestResource(exported = false)
    @Query("select p from Participation p where " + LATEST_CLAUSE)
    Optional<Participation> findLatestByParticipantIdAndMeetupId(@Param("participantId") String participantId,
                                                                 @Param("meetupId") Long meetupId);

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
