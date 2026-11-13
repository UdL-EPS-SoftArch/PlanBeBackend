package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of membership statuses, managed at runtime through /membershipStatuses. */
@RepositoryRestResource
public interface MembershipStatusRepository extends CrudRepository<MembershipStatus, String>, PagingAndSortingRepository<MembershipStatus, String> {
}
