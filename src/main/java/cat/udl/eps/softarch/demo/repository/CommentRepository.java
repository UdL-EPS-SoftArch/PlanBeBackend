package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface CommentRepository extends CrudRepository<Comment, Long>, PagingAndSortingRepository<Comment, Long> {

    /** A comment is visible exactly when its meetup is visible (see MeetupRepository.VISIBILITY_CLAUSE). */
    String VISIBILITY_CLAUSE =
        "c.meetup.visibility = 'PUBLIC' " +
        "or c.meetup.organizer.id = ?#{authentication.name} " +
        "or ?#{hasRole('ADMIN')} = true " +
        "or exists (select i from Invitation i where i.meetup = c.meetup and i.invitee.id = ?#{authentication.name} " +
        "and i.status in (cat.udl.eps.softarch.demo.domain.enums.InvitationStatus.PENDING, " +
        "cat.udl.eps.softarch.demo.domain.enums.InvitationStatus.ACCEPTED)) " +
        "or exists (select p from Participation p where p.meetup = c.meetup and p.participant.id = ?#{authentication.name} " +
        "and p.status in (cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus.CONFIRMED, " +
        "cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus.WAITING))";

    @Override
    @Query("SELECT c FROM Comment c WHERE " + VISIBILITY_CLAUSE)
    Page<Comment> findAll(Pageable pageable);

    @Override
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND (" + VISIBILITY_CLAUSE + ")")
    Optional<Comment> findById(@Param("id") Long id);

    @RestResource(exported = false)
    List<Comment> findByMeetupId(Long meetupId);
}
