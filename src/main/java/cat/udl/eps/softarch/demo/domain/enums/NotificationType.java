package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Notification types change as new features appear, so they live in a catalog table where a new type can be
 * added at runtime, together with its display metadata, without redeploying the application.
 */
@Entity
@Table(name = "NotificationType")
@Getter
@Setter
public class NotificationType extends CatalogEntity {

    public static final NotificationType INVITATION = new NotificationType("INVITATION");
    public static final NotificationType MEETUP_CANCELLED = new NotificationType("MEETUP_CANCELLED");
    public static final NotificationType MEETUP_UPDATED = new NotificationType("MEETUP_UPDATED");
    public static final NotificationType SPOT_FREED = new NotificationType("SPOT_FREED");
    public static final NotificationType GROUP_REQUEST = new NotificationType("GROUP_REQUEST");

    private String description;
    private String icon;

    public NotificationType() {
    }

    public NotificationType(String id) {
        super(id);
    }

    public static NotificationType valueOf(String name) {
        return new NotificationType(name);
    }

    public static List<NotificationType> values() {
        return List.of(INVITATION, MEETUP_CANCELLED, MEETUP_UPDATED, SPOT_FREED, GROUP_REQUEST);
    }
}
