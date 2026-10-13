package cat.udl.eps.softarch.demo.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "Meetup")
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

    public enum Visibility {
        PUBLIC, PRIVATE, GROUP
    }

    public enum MeetupStatus {
        DRAFT, OPEN, FULL, CANCELLED, FINISHED
    }
}
