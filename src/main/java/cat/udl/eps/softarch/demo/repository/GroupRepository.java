package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface GroupRepository extends CrudRepository<Group, Long>, PagingAndSortingRepository<Group, Long> {

    String OWNERSHIP_CLAUSE =
        "g.ownedBy.id = ?#{authentication.name} " +
        "OR g.visibility = 'PUBLIC' " +
        "OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT g FROM Group g WHERE " + OWNERSHIP_CLAUSE)
    Page<Group> findAll(Pageable pageable);

    @Override
    @Query("SELECT g FROM Group g WHERE g.id = :id AND (" + OWNERSHIP_CLAUSE + ")")
    Optional<Group> findById(@Param("id") Long id);

    @Query("SELECT g FROM Group g WHERE g.ownedBy = :user AND (" + OWNERSHIP_CLAUSE + ")")
    Page<Group> findByOwnedBy(@Param("user") User owner, Pageable pageable);

    List<Group> findByName(String name);
}
