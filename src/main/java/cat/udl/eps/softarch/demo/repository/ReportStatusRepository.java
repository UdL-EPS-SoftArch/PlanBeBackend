package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.ReportStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of moderation report statuses, managed at runtime through /reportStatuses. */
@RepositoryRestResource
public interface ReportStatusRepository extends CrudRepository<ReportStatus, String>, PagingAndSortingRepository<ReportStatus, String> {
}
