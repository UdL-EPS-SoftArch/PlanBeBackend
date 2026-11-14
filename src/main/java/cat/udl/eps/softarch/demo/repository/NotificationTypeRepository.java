package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** Catalog of notification types, managed at runtime through /notificationTypes. */
@RepositoryRestResource
public interface NotificationTypeRepository extends CrudRepository<NotificationType, String>, PagingAndSortingRepository<NotificationType, String> {
}
