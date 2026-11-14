package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Invitation statuses are catalog rows so that dynamic metadata, such as the days before an invitation
 * expires automatically, can be configured without redeploying the application.
 */
@Entity
@Table(name = "InvitationStatus")
@Getter
@Setter
public class InvitationStatus extends CatalogEntity {

    public static final InvitationStatus PENDING = new InvitationStatus("PENDING");
    public static final InvitationStatus ACCEPTED = new InvitationStatus("ACCEPTED");
    public static final InvitationStatus DECLINED = new InvitationStatus("DECLINED");

    private String description;
    private Integer expirationDays;

    public InvitationStatus() {
    }

    public InvitationStatus(String id) {
        super(id);
    }

    public static InvitationStatus valueOf(String name) {
        return new InvitationStatus(name);
    }

    public static List<InvitationStatus> values() {
        return List.of(PENDING, ACCEPTED, DECLINED);
    }
}
