package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Meetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RepositoryRestResource
@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    /**
     * Find participation by participant and meetup.
     * Used to prevent duplicate joins.
     */
    Optional<Participation> findByParticipantIdAndMeetupId(String participantId, Long meetupId);

    /**
     * Find participations for the current authenticated user.
     * Row-level authorization via SpEL.
     */
    @Query("select p from Participation p where p.participant.id = ?#{authentication.name} or ?#{hasRole('ADMIN')} = true")
    Iterable<Participation> findMyParticipations();

    long countByMeetupId(Long meetupId);
}
