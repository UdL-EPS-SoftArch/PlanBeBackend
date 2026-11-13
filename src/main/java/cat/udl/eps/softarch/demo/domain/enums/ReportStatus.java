package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Moderation flows evolve quickly, so report statuses live in a catalog table that can grow with new states
 * such as EN_REVISION or ESCALADO without a schema migration.
 */
@Entity
@Table(name = "ReportStatus")
@Getter
@Setter
public class ReportStatus extends CatalogEntity {

    public static final ReportStatus OPEN = new ReportStatus("OPEN");
    public static final ReportStatus RESOLVED = new ReportStatus("RESOLVED");
    public static final ReportStatus DISMISSED = new ReportStatus("DISMISSED");

    private String description;
    private String color;

    public ReportStatus() {
    }

    public ReportStatus(String id) {
        super(id);
    }

    public static ReportStatus valueOf(String name) {
        return new ReportStatus(name);
    }

    public static List<ReportStatus> values() {
        return List.of(OPEN, RESOLVED, DISMISSED);
    }
}
