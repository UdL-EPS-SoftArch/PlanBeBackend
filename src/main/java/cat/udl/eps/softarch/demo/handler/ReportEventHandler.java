package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Report;
import cat.udl.eps.softarch.demo.domain.ReportStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ReviewRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * #49 Report content and #50 report management. Any user reports a user, a meetup or a comment (review):
 * the report is stored OPEN with the caller as reporter. Only an ADMIN changes a report, and only its status,
 * from OPEN to RESOLVED or DISMISSED. WebSecurityConfig already limits PATCH/PUT/DELETE on /reports to ADMIN;
 * the checks below are the defence in depth.
 */
@Component
@RepositoryEventHandler
public class ReportEventHandler {
    final UserRepository userRepository;
    final MeetupRepository meetupRepository;
    final ReviewRepository reviewRepository;

    public ReportEventHandler(UserRepository userRepository, MeetupRepository meetupRepository,
                              ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.meetupRepository = meetupRepository;
        this.reviewRepository = reviewRepository;
    }

    @HandleBeforeCreate
    public void handleReportPreCreate(Report report) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User reporter = userRepository.findById(currentUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // A blank reason is answered by Bean Validation ("must not be blank"); a bad target is checked here
        // too because this handler may run before the validator.
        String targetUri = report.getTargetUri();
        if (targetUri != null && !targetUri.isBlank()) {
            if (!targetUri.matches(Report.TARGET_URI_PATTERN)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "targetUri must be the URI of a user, a meetup or a review");
            }
            // "http://host/reviews/3" (the API's own links) is stored as "/reviews/3"
            targetUri = toPath(targetUri);
            if (targetUri == null || !targetExists(targetUri)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The reported content does not exist");
            }
            report.setTargetUri(targetUri);
        }

        report.setReporter(reporter);
        report.setStatus(ReportStatus.OPEN);
        report.setCreated(Instant.now());
    }

    @HandleBeforeSave
    public void handleReportPreSave(Report report) {
        Report.LoadedState before = report.loadedState();
        if (before == null) {
            // Only reachable if the entity was not loaded from the DB: refuse instead of guessing.
            throw new IllegalStateException("Report " + report.getId() + " has no loaded state");
        }
        checkIsAdmin();

        String reporterId = report.getReporter() == null ? null : report.getReporter().getId();
        boolean onlyStatusChanged = Objects.equals(before.reporterId(), reporterId)
            && Objects.equals(before.targetUri(), report.getTargetUri())
            && Objects.equals(before.reason(), report.getReason())
            && Objects.equals(before.created(), report.getCreated());
        if (!onlyStatusChanged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the status of a report can be changed");
        }

        if (before.status() != ReportStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already " + before.status());
        }

        if (report.getStatus() != ReportStatus.RESOLVED && report.getStatus() != ReportStatus.DISMISSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A report can only be marked RESOLVED or DISMISSED");
        }
    }

    @HandleBeforeDelete
    public void handleReportPreDelete(Report report) {
        checkIsAdmin();
    }

    @HandleBeforeLinkSave
    public void handleReportPreLinkSave(Report report, Object linked) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The reporter of a report cannot be changed");
    }

    @HandleBeforeLinkDelete
    public void handleReportPreLinkDelete(Report report, Object linked) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The reporter of a report cannot be changed");
    }

    private static void checkIsAdmin() {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an ADMIN can manage reports");
        }
    }

    /**
     * targetUri is a path "/{collection}/{id}". Only existence is checked, not whether the caller can read the
     * content now: "any user reports" (#49), e.g. someone removed from a private meetup reports it or its reviews.
     * Nothing about the target is returned, so this does not expose a private meetup.
     */
    private boolean targetExists(String targetUri) {
        String[] parts = targetUri.split("/");
        if (parts.length != 3) return false; // e.g. an encoded "/" inside the id
        String collection = parts[1];
        String id = parts[2];
        if (collection.equals("users")) {
            return userRepository.existsById(id);
        }
        Long numericId = parseId(id);
        if (numericId == null) return false;
        if (collection.equals("meetups")) {
            return meetupRepository.existsById(numericId);
        }
        return reviewRepository.existsById(numericId);
    }

    private static String toPath(String targetUri) {
        try {
            return URI.create(targetUri).getPath();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
