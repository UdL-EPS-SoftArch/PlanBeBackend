package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.InvitationStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of invitation statuses, managed at runtime through /invitationStatuses. */
@RepositoryRestResource
public interface InvitationStatusRepository extends CrudRepository<InvitationStatus, String>, PagingAndSortingRepository<InvitationStatus, String> {
}
