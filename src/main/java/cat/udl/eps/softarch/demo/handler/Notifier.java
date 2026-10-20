package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Notification;
import cat.udl.eps.softarch.demo.domain.NotificationType;
import cat.udl.eps.softarch.demo.domain.UriEntity;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The only place where notifications are created. Used by the event handlers.
 * Bad input is a programming error: it throws instead of storing a broken notification
 * (no "/meetups/null" links, no notifications without recipient).
 */
@Component
public class Notifier {

    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    private final NotificationRepository notificationRepository;

    public Notifier(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification notify(User recipient, NotificationType type, String message, UriEntity<?> target) {
        if (recipient == null) throw new IllegalArgumentException("Notification recipient is required");
        return notifyAll(List.of(recipient), null, type, message, target).getFirst();
    }

    /**
     * Notifies each distinct recipient once, skipping the user that caused the event.
     * All notifications are stored in one transaction: all or none.
     *
     * @param excludedUsername user that triggered the event (may be null)
     * @return the stored notifications
     */
    @Transactional
    public List<Notification> notifyAll(Collection<User> recipients, String excludedUsername,
                                        NotificationType type, String message, UriEntity<?> target) {
        Objects.requireNonNull(recipients, "recipients must not be null");
        if (type == null) throw new IllegalArgumentException("Notification type is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Notification message is required");
        String targetUri = uriOf(target);

        Map<String, User> distinct = new LinkedHashMap<>();
        for (User recipient : recipients) {
            if (recipient == null || recipient.getId() == null) {
                throw new IllegalArgumentException("Notification recipient must be a persisted user");
            }
            if (!recipient.getId().equals(excludedUsername)) distinct.putIfAbsent(recipient.getId(), recipient);
        }

        Instant now = Instant.now();
        List<Notification> notifications = new ArrayList<>();
        for (User recipient : distinct.values()) {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setType(type);
            notification.setMessage(truncate(message));
            notification.setTargetUri(targetUri);
            notification.setRead(false);
            notification.setCreated(now);
            notifications.add(notification);
        }
        List<Notification> saved = new ArrayList<>();
        notificationRepository.saveAll(notifications).forEach(saved::add);
        logger.info("Sent {} {} notification(s) about {}", saved.size(), type, targetUri);
        return saved;
    }

    private static String uriOf(UriEntity<?> target) {
        if (target == null || target.getId() == null) {
            throw new IllegalArgumentException("Notification target must be a persisted entity");
        }
        String uri = target.getUri();
        if (uri.length() > Notification.MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Notification target URI too long: " + uri);
        }
        return uri;
    }

    private static String truncate(String message) {
        String trimmed = message.strip();
        return trimmed.length() <= Notification.MAX_TEXT_LENGTH ? trimmed
            : trimmed.substring(0, Notification.MAX_TEXT_LENGTH - 1) + "…";
    }
}
