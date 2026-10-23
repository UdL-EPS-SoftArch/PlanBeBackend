package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Comment;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import cat.udl.eps.softarch.demo.security.MeetupSecurity;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RepositoryEventHandler
public class CommentEventHandler {

    private final UserRepository userRepository;
    private final MeetupSecurity meetupSecurity;

    public CommentEventHandler(UserRepository userRepository, MeetupSecurity meetupSecurity) {
        this.userRepository = userRepository;
        this.meetupSecurity = meetupSecurity;
    }

    @HandleBeforeCreate
    public void handleCommentPreCreate(Comment comment) {
        Meetup meetup = comment.getMeetup();
        if (meetup == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? null : authentication.getName();
        User author = username == null ? null : userRepository.findById(username).orElse(null);
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to comment");
        }
        comment.setAuthor(author);

        // Only users that can see the meetup may comment it. Hidden meetups answer 404, not 403,
        // so the existence of a private meetup is not disclosed.
        if (!meetupSecurity.canRead(meetup.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found");
        }
    }

    @HandleBeforeSave
    public void handleCommentPreSave(Comment comment) {
        requireAuthorOrAdmin(comment);
    }

    @HandleBeforeDelete
    public void handleCommentPreDelete(Comment comment) {
        requireAuthorOrAdmin(comment);
    }

    private void requireAuthorOrAdmin(Comment comment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        String username = authentication == null ? null : authentication.getName();
        String authorId = comment.getAuthor() == null ? null : comment.getAuthor().getId();
        if (username == null || !username.equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or an ADMIN can modify a comment");
        }
    }
}
