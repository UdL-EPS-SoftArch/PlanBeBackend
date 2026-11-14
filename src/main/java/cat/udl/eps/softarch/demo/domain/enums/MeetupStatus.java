package cat.udl.eps.softarch.demo.domain.enums;

import cat.udl.eps.softarch.demo.domain.entities.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Meetup lifecycle statuses are catalog rows so that frontend metadata and behaviour flags (for example,
 * whether the chat accepts comments) can change at runtime.
 */
@Entity
@Table(name = "MeetupStatus")
@Getter
@Setter
public class MeetupStatus extends CatalogEntity {

    public static final MeetupStatus DRAFT = new MeetupStatus("DRAFT");
    public static final MeetupStatus OPEN = new MeetupStatus("OPEN");
    public static final MeetupStatus FULL = new MeetupStatus("FULL");
    public static final MeetupStatus CANCELLED = new MeetupStatus("CANCELLED");
    public static final MeetupStatus FINISHED = new MeetupStatus("FINISHED");

    private String description;
    private String color;
    private boolean allowsComments = true;

    public MeetupStatus() {
    }

    public MeetupStatus(String id) {
        super(id);
    }

    public static MeetupStatus valueOf(String name) {
        return new MeetupStatus(name);
    }

    public static List<MeetupStatus> values() {
        return List.of(DRAFT, OPEN, FULL, CANCELLED, FINISHED);
    }
}
