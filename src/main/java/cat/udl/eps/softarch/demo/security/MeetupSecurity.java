package cat.udl.eps.softarch.demo.security;

import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component("meetupSecurity")
public class MeetupSecurity {

    private final MeetupRepository meetupRepository;
    private final InvitationRepository invitationRepository;
    private final ParticipationRepository participationRepository;

    public MeetupSecurity(MeetupRepository meetupRepository, InvitationRepository invitationRepository,
                          ParticipationRepository participationRepository) {
        this.meetupRepository = meetupRepository;
        this.invitationRepository = invitationRepository;
        this.participationRepository = participationRepository;
    }

    public boolean canRead(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return true;
        if (username != null && meetupRepository.findByIdAndOwner(id, username).isPresent()) return true;
        if (username != null && isInvitedOrParticipant(id, username)) return true;

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
    /** Invitees with a PENDING or ACCEPTED invitation and active participants see the meetup (#28). */
    private boolean isInvitedOrParticipant(Long meetupId, String username) {
        return invitationRepository.existsByMeetupIdAndInviteeIdAndStatusIn(meetupId, username,
                EnumSet.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED))
            || participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(username, meetupId,
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING));
    }
}
