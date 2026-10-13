package cat.udl.eps.softarch.demo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)
public class Meetup extends UriEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Future
    private Instant startDate;

    @NotNull
    private Instant endDate;

    @Min(1)
    private Integer capacity;

    @Embedded
    private Location location;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @CreatedDate
    private Instant created;

    @LastModifiedDate
    private Instant modified;

    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }
}
