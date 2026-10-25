package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Membership;
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
public interface MembershipRepository extends CrudRepository<Membership, Long>, PagingAndSortingRepository<Membership, Long> {

    // A membership is visible to the member itself, to the owner of the group and to the admins of that
    // group (a membership with the OWNER or the ADMIN role), plus the platform admins.
    String OWNERSHIP_CLAUSE =
        "m.member.id = ?#{authentication.name} " +
        "OR m.group.ownedBy.id = ?#{authentication.name} " +
        "OR EXISTS (SELECT gm FROM Membership gm WHERE gm.group.id = m.group.id " +
        "AND gm.member.id = ?#{authentication.name} " +
        "AND gm.role IN (cat.udl.eps.softarch.demo.domain.GroupRole.OWNER, cat.udl.eps.softarch.demo.domain.GroupRole.ADMIN)) " +
        "OR ?#{hasRole('ADMIN')} = true";

    @Override
    @Query("SELECT m FROM Membership m WHERE " + OWNERSHIP_CLAUSE)
    Page<Membership> findAll(Pageable pageable);

    @Override
    @Query("SELECT m FROM Membership m WHERE m.id = :id AND (" + OWNERSHIP_CLAUSE + ")")
    Optional<Membership> findById(@Param("id") Long id);

    @Query("SELECT m FROM Membership m WHERE m.group.id = :groupId AND " + OWNERSHIP_CLAUSE)
    Page<Membership> findByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    // Internal use only: who may read or change a membership is decided by MembershipEventHandler.
    @RestResource(exported = false)
    Optional<Membership> findByGroupIdAndMemberId(Long groupId, String memberId);

    /**
     * Internal use only (no SpEL, so never exported): the memberships that can manage a group, i.e. the
     * ACTIVE rows with the OWNER or ADMIN role. Used to notify the group managers of a join request.
     */
    @RestResource(exported = false)
    @Query("SELECT m FROM Membership m WHERE m.group.id = :groupId " +
        "AND m.status = cat.udl.eps.softarch.demo.domain.enums.MembershipStatus.ACTIVE " +
        "AND m.role IN (cat.udl.eps.softarch.demo.domain.GroupRole.OWNER, " +
        "cat.udl.eps.softarch.demo.domain.GroupRole.ADMIN)")
    List<Membership> findActiveManagersByGroupId(@Param("groupId") Long groupId);
}
