package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import cat.udl.eps.softarch.demo.domain.enums.Recurrence;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
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
  
    private Integer minAge;

    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    private Gender targetGender;

    @DecimalMin("0.0")
    private BigDecimal price;

    // Validated at the REST boundary only (jakarta.persistence.validation.mode: none), so internal saves
    // of meetups without dates (e.g. test fixtures, WaitingList status updates) are not rejected.
    // A future start is only required when creating (MeetupEventHandler): a meetup that already started
    // must still be editable, e.g. to mark it FINISHED.
    @NotNull
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

    /** How often the meetup repeats (#52). The last occurrence is the one starting on recurrenceEnd or before. */
    @Enumerated(EnumType.STRING)
    private Recurrence recurrence = Recurrence.NONE;

    private LocalDate recurrenceEnd;

    /** Occurrence this one was generated from when it was FINISHED (#53). Set by the system only. */
    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Meetup previousOccurrence;

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

    @JsonIgnore
    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @JsonIgnore
    @AssertTrue(message = "recurrenceEnd requires a recurrence")
    public boolean isRecurrenceEndWithRecurrence() {
        return recurrenceEnd == null || (recurrence != null && recurrence != Recurrence.NONE);
    }

    @AssertTrue(message = "recurrenceEnd must not be before the start date")
    public boolean isRecurrenceEndAfterStartDate() {
        if (recurrenceEnd == null || startDate == null) return true;
        return !recurrenceEnd.isBefore(LocalDate.ofInstant(startDate, ZoneOffset.UTC));
    }

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
