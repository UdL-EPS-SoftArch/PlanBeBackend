package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of participation statuses, managed at runtime through /participationStatuses. */
@RepositoryRestResource
public interface ParticipationStatusRepository extends CrudRepository<ParticipationStatus, String>, PagingAndSortingRepository<ParticipationStatus, String> {
}
