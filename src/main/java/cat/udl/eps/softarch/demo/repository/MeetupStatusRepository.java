package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of meetup lifecycle statuses, managed at runtime through /meetupStatuses. */
@RepositoryRestResource
public interface MeetupStatusRepository extends CrudRepository<MeetupStatus, String>, PagingAndSortingRepository<MeetupStatus, String> {
}
