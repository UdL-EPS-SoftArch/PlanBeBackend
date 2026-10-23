package cat.udl.eps.softarch.demo.domain;

import cat.udl.eps.softarch.demo.domain.Entity.UriEntity;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * #49: any user reports a user, a meetup or a comment (the text of a review). Reports are created OPEN and
 * only an ADMIN marks them RESOLVED or DISMISSED (#50, see ReportEventHandler).
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class Report extends UriEntity<Long> {

    public static final int MAX_REASON_LENGTH = 500;

    /**
     * What can be reported: "/users/{id}", "/meetups/{id}" or "/reviews/{id}" (see UriEntity#getUri), or the
     * absolute link the API returns ("http://host/reviews/3", i.e. _links.self.href). It is stored as the path.
     */
    public static final String TARGET_URI_PATTERN = "^(https?://[^/?#]+)?/(users|meetups|reviews)/[^/?#]+$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always the authenticated user that creates the report (set by ReportEventHandler). */
    @ManyToOne
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private User reporter;

    /** URI of the reported user, meetup or comment, stored as a path, e.g. "/reviews/3". */
    @NotBlank
    @Pattern(regexp = TARGET_URI_PATTERN, message = "must be the URI of a user, a meetup or a review")
    @Column(nullable = false)
    private String targetUri;

    @NotBlank
    @Length(max = MAX_REASON_LENGTH)
    @Column(nullable = false, length = MAX_REASON_LENGTH)
    private String reason;

    /** OPEN when created, whatever the client sends; only an ADMIN changes it. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @DateTimeFormat
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant created;

    /** State as loaded from the database, used by ReportEventHandler to allow only status changes. */
    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LoadedState loadedState;

    public record LoadedState(String reporterId, String targetUri, String reason, ReportStatus status,
                              Instant created) {}

    @PostLoad
    void captureLoadedState() {
        loadedState = new LoadedState(reporter == null ? null : reporter.getId(), targetUri, reason, status, created);
    }

    /** @return the persisted state, or null if this instance was not loaded from the database. */
    public LoadedState loadedState() {
        return loadedState;
    }
}
