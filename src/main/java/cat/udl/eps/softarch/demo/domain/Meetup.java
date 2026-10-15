package cat.udl.eps.softarch.demo.domain;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import cat.udl.eps.softarch.demo.domain.enums.Category;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.MeetupVisibility;

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

    @Valid
    @Embedded
    private Location location;

    @ManyToOne
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private MeetupStatus status = MeetupStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private MeetupVisibility visibility = MeetupVisibility.PUBLIC;

    private Integer minAge;
    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    private Gender targetGender;

    @CreatedDate
    private Instant created;

    @LastModifiedDate
    private Instant modified;

    @AssertTrue(message = "endDate must be after startDate")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "minAge must be less than or equal to maxAge")
    public boolean isAgeRangeValid() {
        if (minAge == null || maxAge == null) return true;
        return minAge <= maxAge;
    }
}
