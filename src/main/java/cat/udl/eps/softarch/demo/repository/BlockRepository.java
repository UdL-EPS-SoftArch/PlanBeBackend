package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

@RepositoryRestResource
public interface BlockRepository extends CrudRepository<Block, Long>, PagingAndSortingRepository<Block, Long> {

    /** The blocker sees (and can delete) their own blocks, an ADMIN all of them. The blocked user is not told. */
    String OWNERSHIP_CLAUSE = "b.blocker.id = ?#{authentication.name} OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT b FROM Block b WHERE " + OWNERSHIP_CLAUSE)
    Page<Block> findAll(Pageable pageable);

    @Override
    @Query("SELECT b FROM Block b WHERE b.id = :id AND (" + OWNERSHIP_CLAUSE + ")")
    Optional<Block> findById(@Param("id") Long id);

    // Used by the Block, Participation and Invitation handlers: has blockerId blocked blockedId?
    @RestResource(exported = false)
    boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);
}
