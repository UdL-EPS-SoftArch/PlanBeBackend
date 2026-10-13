package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Meetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@RepositoryRestResource
@Repository
public interface MeetupRepository extends JpaRepository<Meetup, Long> {
    Optional<Meetup> findByTitle(String title);
}
