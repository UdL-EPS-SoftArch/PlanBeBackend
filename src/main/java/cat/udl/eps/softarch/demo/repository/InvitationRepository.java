package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Collection;
import java.util.Optional;

@RepositoryRestResource
public interface InvitationRepository extends CrudRepository<Invitation, Long>, PagingAndSortingRepository<Invitation, Long> {

    String VISIBILITY_CLAUSE =
        "i.invitee.id = ?#{authentication.name} " +
        "OR i.invitedBy.id = ?#{authentication.name} " +
        "OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT i FROM Invitation i WHERE " + VISIBILITY_CLAUSE)
    Page<Invitation> findAll(Pageable pageable);

    @Override
    @Query("SELECT i FROM Invitation i WHERE i.id = :id AND (" + VISIBILITY_CLAUSE + ")")
    Optional<Invitation> findById(@Param("id") Long id);

    Optional<Invitation> findByInviteeIdAndMeetupId(String inviteeId, Long meetupId);

    boolean existsByInviteeIdAndMeetupId(String inviteeId, Long meetupId);

    // Stored values, used to compare against the incoming changes before saving
    @RestResource(exported = false)
    @Query("SELECT i.status FROM Invitation i WHERE i.id = :id")
    InvitationStatus findStoredStatusById(@Param("id") Long id);

    @RestResource(exported = false)
    boolean existsByIdAndInviteeIdAndMeetupId(Long id, String inviteeId, Long meetupId);

    // Used by MeetupSecurity: invitees see the private meetups they are invited to
    @RestResource(exported = false)
    boolean existsByMeetupIdAndInviteeIdAndStatusIn(Long meetupId, String inviteeId, Collection<InvitationStatus> statuses);
}
