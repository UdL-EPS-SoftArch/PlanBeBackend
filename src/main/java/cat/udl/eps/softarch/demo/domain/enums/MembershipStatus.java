package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Membership statuses are catalog rows so that metadata such as an automatic expiration margin can be
 * adjusted at runtime.
 */
@Entity
@Table(name = "MembershipStatus")
@Getter
@Setter
public class MembershipStatus extends CatalogEntity {

    public static final MembershipStatus PENDING = new MembershipStatus("PENDING");
    public static final MembershipStatus ACTIVE = new MembershipStatus("ACTIVE");

    private String description;
    private Integer expirationDays;

    public MembershipStatus() {
    }

    public MembershipStatus(String id) {
        super(id);
    }

    public static MembershipStatus valueOf(String name) {
        return new MembershipStatus(name);
    }

    public static List<MembershipStatus> values() {
        return List.of(PENDING, ACTIVE);
    }
}
