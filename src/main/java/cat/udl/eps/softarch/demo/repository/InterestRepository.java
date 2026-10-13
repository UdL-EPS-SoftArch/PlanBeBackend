package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Interest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface InterestRepository extends CrudRepository<Interest, Long>, PagingAndSortingRepository<Interest, Long> {

    Optional<Interest> findByName(@Param("name") String name);

    List<Interest> findByNameContainingIgnoreCase(@Param("text") String text);
}
