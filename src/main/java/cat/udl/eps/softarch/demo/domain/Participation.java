package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "Participation", uniqueConstraints = @UniqueConstraint(
    // At most one active (CONFIRMED or WAITING) participation per user and meetup, enforced by the database.
    // active_slot is TRUE while active and NULL once CANCELLED; NULLs never collide in a unique constraint,
    // so the CANCELLED rows kept as history do not block joining again.
    name = "uk_participation_active", columnNames = {"participant_id", "meetup_id", "active_slot"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Participation extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meetup_id", nullable = false)
    private Meetup meetup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    /** TRUE while the participation is CONFIRMED or WAITING, NULL when CANCELLED (see the unique constraint). */
    @Column(name = "active_slot")
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Boolean activeSlot;

    @Column(nullable = false)
    private Instant joinedAt;

    public void setStatus(ParticipationStatus status) {
        this.status = status;
        syncActiveSlot();
    }

    @PrePersist
    @PreUpdate
    void syncActiveSlot() {
        activeSlot = status == null || status == ParticipationStatus.CANCELLED ? null : Boolean.TRUE;
    }

    /** State as loaded from the database, used by ParticipationEventHandler to detect transitions. */
    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LoadedState loadedState;

    public record LoadedState(String participantId, Long meetupId, ParticipationStatus status, Instant joinedAt) {}

    @PostLoad
    public void captureLoadedState() {
        loadedState = new LoadedState(participant == null ? null : participant.getId(),
            meetup == null ? null : meetup.getId(), status, joinedAt);
    }

    /** @return the persisted state, or null if this instance was not loaded from the database. */
    public LoadedState loadedState() {
        return loadedState;
    }
}
