package cat.udl.eps.softarch.demo.security;

import cat.udl.eps.softarch.demo.domain.Meetup;
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

    public boolean isOwner(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) return true;

        return meetupRepository.findByIdAndOwner(id, username).isPresent();
    }
}
