package cat.udl.eps.softarch.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

@Entity
@Table(name = "Meetup")
// Only changed columns are written: WaitingList (status) and an organizer's PATCH (e.g. location)
// running at the same time cannot overwrite each other's columns with stale values.
@DynamicUpdate
@Data
@EqualsAndHashCode(callSuper = true)
public class Meetup extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetupStatus status;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;
  
    private Integer minAge;

    private Integer maxAge;

    // Same names/types as branch 9-épica-quedadas-meetup (#23) so both merge cleanly.
    // Nullable on purpose: validations (@NotNull, @Future, endDate > startDate) belong to #23.
    private Instant startDate;

    private Instant endDate;

    @Valid
    @Embedded
    private Location location;

    /** State as loaded from the database, used by MeetupEventHandler to detect changes. */
    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LoadedState loadedState;

    public enum Visibility {
        PUBLIC, PRIVATE, GROUP
    }

    public enum MeetupStatus {
        DRAFT, OPEN, FULL, CANCELLED, FINISHED
    }

    public record LoadedState(MeetupStatus status, Integer capacity, Instant startDate, Instant endDate,
                              Location location, String organizerId) {}

    /**
     * Snapshot of the persisted state. Location is copied because a PATCH merges
     * the new values into the same embedded instance.
     */
    @PostLoad
    public void captureLoadedState() {
        Location locationCopy = location == null ? null : new Location(location.getName(),
            location.getAddress(), location.getCity(), location.getLatitude(), location.getLongitude());
        loadedState = new LoadedState(status, capacity, startDate, endDate, locationCopy,
            organizer == null ? null : organizer.getId());
    }

    /** @return the persisted state, or null if this instance was not loaded from the database. */
    public LoadedState loadedState() {
        return loadedState;
    }
}
