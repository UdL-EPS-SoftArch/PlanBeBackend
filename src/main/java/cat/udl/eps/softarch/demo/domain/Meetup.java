package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import cat.udl.eps.softarch.demo.domain.enums.Gender;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String description;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetupStatus status = MeetupStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    /**
     * RF-5.4: the group that owns a GROUP meetup. Null for PUBLIC and PRIVATE meetups.
     */
    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIdentityReference(alwaysAsId = true)
    private Group group;
  
    private Integer minAge;

    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    private Gender targetGender;

    @DecimalMin("0.0")
    private BigDecimal price;

    // Validated at the REST boundary only (jakarta.persistence.validation.mode: none), so internal saves
    // of meetups without dates (e.g. test fixtures, WaitingList status updates) are not rejected.
    @NotNull
    @Future
    private Instant startDate;

    @NotNull
    private Instant endDate;

    @Valid
    @Embedded
    private Location location;

    /** Interests of the meetup, matched against the user's interests for recommendations (#46). */
    @ManyToMany
    @JsonIdentityReference(alwaysAsId = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Interest> tags = new HashSet<>();

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
                              Location location, String organizerId, Long groupId) {}

    /**
     * Snapshot of the persisted state. Location is copied because a PATCH merges
     * the new values into the same embedded instance.
     */
    @PostLoad
    public void captureLoadedState() {
        Location locationCopy = location == null ? null : new Location(location.getName(),
            location.getAddress(), location.getCity(), location.getLatitude(), location.getLongitude());
        loadedState = new LoadedState(status, capacity, startDate, endDate, locationCopy,
            organizer == null ? null : organizer.getId(),
            group == null ? null : group.getId());
    }

    @JsonIgnore
    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @JsonIgnore
    @AssertTrue(message = "minAge must be less than or equal to maxAge")
    public boolean isAgeRangeValid() {
        if (minAge == null || maxAge == null) return true;
        return minAge <= maxAge;
    }

    /** @return the persisted state, or null if this instance was not loaded from the database. */
    public LoadedState loadedState() {
        return loadedState;
    }
}
