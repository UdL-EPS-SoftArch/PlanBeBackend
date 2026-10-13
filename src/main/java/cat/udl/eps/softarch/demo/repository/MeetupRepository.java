package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Meetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface MeetupRepository extends JpaRepository<Meetup, Long> {

    @Query("select m from Meetup m where m.organizer.id = ?#{authentication.name} or ?#{hasRole('ADMIN')} = true")
    List<Meetup> findMyMeetups();

    @Query("select m from Meetup m where m.id = ?#{#id} and (m.organizer.id = ?#{#username} or ?#{hasRole('ADMIN')} = true)")
    Optional<Meetup> findByIdAndOwner(Long id, String username);

    @Override
    @PreAuthorize("@meetupSecurity.isOwner(#id)")
    Optional<Meetup> findById(Long id);


    @Override
    @PreAuthorize("hasRole('ADMIN') or #entity.organizer.id == authentication.name")
    <S extends Meetup> S save(S entity);

    @Override
    @PreAuthorize("hasRole('ADMIN') or #entity.organizer.id == authentication.name")
    void delete(Meetup entity);
}
