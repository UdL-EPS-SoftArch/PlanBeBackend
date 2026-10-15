package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RepositoryEventHandler
@RequiredArgsConstructor
public class MeetupEventHandler {

    private final UserRepository userRepository;

    @HandleBeforeCreate
    @Transactional
    public void handleBeforeCreate(Meetup meetup) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User organizer = userRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        meetup.setOrganizer(organizer);
    }

    @HandleBeforeSave
    @Transactional
    public void handleBeforeSave(Meetup meetup) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if (meetup.getOrganizer() == null || !meetup.getOrganizer().getId().equals(username)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
        }
    }

    @HandleBeforeDelete
    @Transactional
    public void handleBeforeDelete(Meetup meetup) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if (meetup.getOrganizer() == null || !meetup.getOrganizer().getId().equals(username)) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
            }
        }
    }
}
