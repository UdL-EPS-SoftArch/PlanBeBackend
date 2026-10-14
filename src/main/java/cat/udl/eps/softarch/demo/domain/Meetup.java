package cat.udl.eps.softarch.demo.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Meetup extends UriEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private MeetupStatus status = MeetupStatus.OPEN;

    @OneToMany(mappedBy = "meetup", cascade = CascadeType.ALL)
    private Set<Participation> participations;

    public enum MeetupStatus {
        OPEN, FULL
    }
}
