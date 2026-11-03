package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.entities.UriEntity;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * #48: the blocker does not want anything to do with the blocked user. A blocked user cannot join the
 * blocker's meetups nor invite the blocker. Blocks cannot be modified: to unblock, DELETE the block.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    // A user blocks another user at most once, enforced by the database
    name = "uk_block_blocker_blocked", columnNames = {"blocker_id", "blocked_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Block extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always the authenticated user that creates the block (set by BlockEventHandler). */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User blocker;

    @NotNull
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    private User blocked;

    @DateTimeFormat
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant created;
}
