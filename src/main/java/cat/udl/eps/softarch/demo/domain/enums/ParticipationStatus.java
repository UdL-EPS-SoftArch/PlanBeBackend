package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Participation statuses are catalog rows so that dynamic metadata, such as the days before a waiting entry
 * expires automatically, can be configured without a deployment.
 */
@Entity
@Table(name = "ParticipationStatus")
@Getter
@Setter
public class ParticipationStatus extends CatalogEntity {

    public static final ParticipationStatus CONFIRMED = new ParticipationStatus("CONFIRMED");
    public static final ParticipationStatus WAITING = new ParticipationStatus("WAITING");
    public static final ParticipationStatus CANCELLED = new ParticipationStatus("CANCELLED");

    private String description;
    private Integer expirationDays;

    public ParticipationStatus() {
    }

    public ParticipationStatus(String id) {
        super(id);
    }

    public static ParticipationStatus valueOf(String name) {
        return new ParticipationStatus(name);
    }

    public static List<ParticipationStatus> values() {
        return List.of(CONFIRMED, WAITING, CANCELLED);
    }
}
