package cat.udl.eps.softarch.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * A request body that links a resource the user may not read (e.g. a PRIVATE meetup in
 * POST /participations or POST /invitations) fails while Spring Data REST resolves the URI, because
 * MeetupRepository.findById is guarded by @PreAuthorize. Spring wraps that AccessDeniedException in
 * HttpMessageNotReadableException (400); it is a permission problem, so answer 403 instead.
 *
 * POST /comments is the exception: RF-7 asks a hidden meetup to answer 404 so its existence is not
 * disclosed, matching CommentRepository (which hides comments of meetups the caller cannot read).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedReferenceHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException exception,
                                                                 HttpServletRequest request) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof AccessDeniedException) {
                if (request.getRequestURI().startsWith("/comments")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Referenced resource not found"));
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied to a resource referenced in the request body"));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(exception.getMostSpecificCause().getMessage())));
    }
}
