package cat.udl.eps.softarch.demo.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Participation extends UriEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "meetup_id")
    private Meetup meetup;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status = ParticipationStatus.CONFIRMED;

    public enum ParticipationStatus {
        CONFIRMED, WAITING
    }
}
