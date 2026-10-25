package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "Membership", uniqueConstraints = @UniqueConstraint(
    // At most one membership per user and group: the row is removed when the member is expelled.
    name = "uk_membership_member_group", columnNames = {"member_id", "group_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Membership extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private Group group;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIdentityReference(alwaysAsId = true)
    private User member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Column(nullable = false)
    private Instant joinedAt;

    /** State as loaded from the database, used by MembershipEventHandler to detect role changes. */
    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LoadedState loadedState;

    public record LoadedState(Long groupId, String memberId, GroupRole role, MembershipStatus status, Instant joinedAt) {}

    @PostLoad
    public void captureLoadedState() {
        loadedState = new LoadedState(group == null ? null : group.getId(),
            member == null ? null : member.getId(), role, status, joinedAt);
    }

    /** @return the persisted state, or null if this instance was not loaded from the database. */
    public LoadedState loadedState() {
        return loadedState;
    }
}
