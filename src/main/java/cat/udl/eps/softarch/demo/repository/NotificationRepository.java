package cat.udl.eps.softarch.demo.repository;

import cat.udl.eps.softarch.demo.domain.Notification;
import cat.udl.eps.softarch.demo.domain.NotificationType;
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
public interface NotificationRepository extends CrudRepository<Notification, Long>, PagingAndSortingRepository<Notification, Long> {

    /** Project rule: the recipient sees their notifications, an ADMIN can inspect any of them (read only). */
    String VISIBILITY_CLAUSE = "n.recipient.id = ?#{authentication.name} OR ?#{hasRole('ADMIN')} = true";

    /** The caller's own notifications, for everyone (an ADMIN included): the inbox. */
    String RECIPIENT_CLAUSE = "n.recipient.id = ?#{authentication.name}";

    @Override
    @Query("SELECT n FROM Notification n WHERE " + VISIBILITY_CLAUSE)
    Page<Notification> findAll(Pageable pageable);

    @Override
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND (" + VISIBILITY_CLAUSE + ")")
    Optional<Notification> findById(@Param("id") Long id);

    /** GET /notifications/search/findUnread : the caller's own unread notifications (their inbox badge). */
    @Query("SELECT n FROM Notification n WHERE n.read = false AND " + RECIPIENT_CLAUSE)
    Page<Notification> findUnread(Pageable pageable);

    // ---- Internal use only (no SpEL, so never exported) ----

    @RestResource(exported = false)
    List<Notification> findByRecipientId(String recipientId);

    @RestResource(exported = false)
    List<Notification> findByRecipientIdAndType(String recipientId, NotificationType type);
}
