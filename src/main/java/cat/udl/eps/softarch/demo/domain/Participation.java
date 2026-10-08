package cat.udl.eps.softarch.demo.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "Participation")
@Data
@EqualsAndHashCode(callSuper = true)
public class Participation extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id")
    private User participant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meetup_id")
    private Meetup meetup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status;

    @Column(nullable = false)
    private Instant joinedAt;
}
