package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.User;
import org.springframework.data.rest.core.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Component
@RepositoryEventHandler
public class UserEventHandler {

    @HandleBeforeCreate
    public void handleBeforeCreate(User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If the user is already authenticated and not anonymous, forbid registration
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Already authenticated users cannot register a new account");
        }
    }
}
