package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface MeetupRepository extends JpaRepository<Meetup, Long> {

    // PUBLIC meetups, or PRIVATE ones for their organizer, ADMIN, invitees with a PENDING or ACCEPTED
    // invitation and active participants (#28). GROUP meetups (#33, RF-5.4) are only visible to the
    // active members of their group.
    String VISIBILITY_CLAUSE =
        "m.visibility = 'PUBLIC' " +
        "or m.organizer.id = ?#{authentication.name} " +
        "or ?#{hasRole('ADMIN')} = true " +
        "or exists (select i from Invitation i where i.meetup = m and i.invitee.id = ?#{authentication.name} " +
        "and i.status in (cat.udl.eps.softarch.demo.domain.InvitationStatus.PENDING, " +
        "cat.udl.eps.softarch.demo.domain.InvitationStatus.ACCEPTED)) " +
        "or exists (select p from Participation p where p.meetup = m and p.participant.id = ?#{authentication.name} " +
        "and p.status in (cat.udl.eps.softarch.demo.domain.ParticipationStatus.CONFIRMED, " +
        "cat.udl.eps.softarch.demo.domain.ParticipationStatus.WAITING)) " +
        "or exists (select ms from Membership ms where ms.group.id = m.group.id " +
        "and ms.member.id = ?#{authentication.name} " +
        "and ms.status = cat.udl.eps.softarch.demo.domain.enums.MembershipStatus.ACTIVE)";

    @Override
    @Query("select m from Meetup m where " + VISIBILITY_CLAUSE)
    Page<Meetup> findAll(Pageable pageable);

    @Query("select m from Meetup m where " + VISIBILITY_CLAUSE)
    List<Meetup> findVisibleMeetups();

    @Query("select m from Meetup m where m.category = ?1 and (" + VISIBILITY_CLAUSE + ")")
    List<Meetup> findByCategoryAndVisibility(cat.udl.eps.softarch.demo.domain.enums.Category category);

    @Query("select m from Meetup m where m.location.city = ?1 and (" + VISIBILITY_CLAUSE + ")")
    List<Meetup> findByCityAndVisibility(String city);

    @Query("select m from Meetup m where m.startDate > ?1 and (" + VISIBILITY_CLAUSE + ")")
    List<Meetup> findByStartDateAfterAndVisibility(java.time.Instant startDate);

    @Query("select m from Meetup m where m.organizer.id = ?1 and (" + VISIBILITY_CLAUSE + ")")
    List<Meetup> findByOrganizerAndVisibility(String organizerId);

    @Query("select m from Meetup m where m.id = ?1 and (m.organizer.id = ?#{authentication.name} or ?#{hasRole('ADMIN')} = true)")
    Optional<Meetup> findByIdAndOwner(Long id, String username);

    @Query("select m from Meetup m where m.id = ?1")
    @RestResource(exported = false)
    Optional<Meetup> findByIdRaw(Long id);

    @Override
    @PreAuthorize("@meetupSecurity.canRead(#id)")
    Optional<Meetup> findById(Long id);


    @Override
    @PreAuthorize("@meetupSecurity.canSave(#entity)")
    <S extends Meetup> S save(S entity);

    @Modifying
    @Query("update Meetup m set m.status = :status where m.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") MeetupStatus status);

    @Override
    @PreAuthorize("hasRole('ADMIN') or #entity.organizer.id == authentication.name")
    void delete(Meetup entity);

    @RestResource(exported = false)
    Optional<Meetup> findByTitle(String title);
}
