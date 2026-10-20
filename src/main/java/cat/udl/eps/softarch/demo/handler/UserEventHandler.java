package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RepositoryEventHandler
public class UserEventHandler {

    final Logger logger = LoggerFactory.getLogger(User.class);

    final UserRepository userRepository;

    public UserEventHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @HandleBeforeCreate
    public void handleUserPreCreate(User user) {
        logger.info("Before creating: {}", user.toString());
        checkNotAlreadyAuthenticated();
    }

    @HandleBeforeSave
    public void handleUserPreSave(User user) {
        logger.info("Before updating: {}", user.toString());
        checkCanModify(user);
    }

    @HandleBeforeDelete
    public void handleUserPreDelete(User user) {
        logger.info("Before deleting: {}", user.toString());
    }

    @HandleBeforeLinkSave
    public void handleUserPreLinkSave(User user, Object o) {
        logger.info("Before linking: {} to {}", user.toString(), o.toString());
        checkCanModify(user);
    }

    @HandleBeforeLinkDelete
    public void handleUserPreLinkDelete(User user, Object o) {
        logger.info("Before unlinking: {} from {}", user.toString(), o.toString());
        checkCanModify(user);
    }

    @HandleAfterCreate
    public void handleUserPostCreate(User user) {
        logger.info("After creating: {}", user.toString());
        user.encodePassword();
        userRepository.save(user);
    }

    @HandleAfterSave
    public void handleUserPostSave(User user) {
        logger.info("After updating: {}", user.toString());
        if (user.isPasswordReset()) {
            user.encodePassword();
            userRepository.save(user);
        }
    }
    private void checkNotAlreadyAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If the user is already authenticated and not anonymous, forbid registration
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Already authenticated users cannot register a new account");
        }
    }

    private void checkCanModify(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !authentication.getName().equals(user.getId())) {
            throw new AccessDeniedException("Only the user can modify their own account");
        }
    }
}
