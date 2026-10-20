package cat.udl.eps.softarch.demo.security;

import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("meetupSecurity")
public class MeetupSecurity {

    private final MeetupRepository meetupRepository;

    public MeetupSecurity(MeetupRepository meetupRepository) {
        this.meetupRepository = meetupRepository;
    }

    public boolean canRead(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return true;
        if (username != null && meetupRepository.findByIdAndOwner(id, username).isPresent()) return true;

        return meetupRepository.findByIdRaw(id)
                .map(meetup -> meetup.getVisibility() == Visibility.PUBLIC)
                .orElse(false);
    }

    public boolean canSave(Meetup meetup) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        if (meetup.getId() == null) return true;

        return meetup.getOrganizer() != null
                && meetup.getOrganizer().getId().equals(authentication.getName());
    }
}
