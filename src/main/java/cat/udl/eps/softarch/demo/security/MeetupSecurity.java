package cat.udl.eps.softarch.demo.security;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.GroupRole;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.domain.Membership;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.MembershipRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Optional;

@Component("meetupSecurity")
public class MeetupSecurity {

    private final MeetupRepository meetupRepository;
    private final InvitationRepository invitationRepository;
    private final ParticipationRepository participationRepository;
    private final MembershipRepository membershipRepository;

    public MeetupSecurity(MeetupRepository meetupRepository, InvitationRepository invitationRepository,
                          ParticipationRepository participationRepository,
                          MembershipRepository membershipRepository) {
        this.meetupRepository = meetupRepository;
        this.invitationRepository = invitationRepository;
        this.participationRepository = participationRepository;
        this.membershipRepository = membershipRepository;
    }

    public boolean canRead(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : null;
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return true;

        Optional<Meetup> raw = meetupRepository.findByIdRaw(id);
        if (raw.isEmpty()) return false;
        Meetup meetup = raw.get();

        // RF-5.4: a GROUP meetup only exists for the active members of its group. Anybody else gets a
        // 404, so that even the existence of the group meetup is not disclosed.
        if (meetup.getVisibility() == Visibility.GROUP) {
            if (isOrganizer(meetup, username) || isActiveGroupMember(meetup.getGroup(), username)) return true;
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meetup not found");
        }

        if (isOrganizer(meetup, username)) return true;
        if (username != null && isInvitedOrParticipant(id, username)) return true;

        return meetup.getVisibility() == Visibility.PUBLIC;
    }

    public boolean canSave(Meetup meetup) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        if (meetup.getId() == null) return true;

        // RF-3.2: the organizer and the admins of the group manage its meetups. What is stored in the
        // database decides, never the organizer or the group sent in the request body.
        Optional<Meetup> raw = meetupRepository.findByIdRaw(meetup.getId());
        if (raw.isEmpty()) return false;
        Meetup stored = raw.get();
        return isOrganizer(stored, authentication.getName())
            || isGroupManager(stored.getGroup(), authentication.getName());
    }

    private static boolean isOrganizer(Meetup meetup, String username) {
        return username != null && meetup.getOrganizer() != null
            && username.equals(meetup.getOrganizer().getId());
    }

    /** RF-5.4: only an ACTIVE membership of the group makes its GROUP meetups readable. */
    private boolean isActiveGroupMember(Group group, String username) {
        if (group == null || group.getId() == null || username == null) return false;
        if (group.getOwnedBy() != null && username.equals(group.getOwnedBy().getId())) return true;
        return membershipRepository.findByGroupIdAndMemberId(group.getId(), username)
            .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
            .isPresent();
    }

    /** The owner of the group and the members with the OWNER or ADMIN role manage its meetups (RF-3.2). */
    private boolean isGroupManager(Group group, String username) {
        if (group == null || group.getId() == null || username == null) return false;
        if (group.getOwnedBy() != null && username.equals(group.getOwnedBy().getId())) return true;
        return membershipRepository.findByGroupIdAndMemberId(group.getId(), username)
            .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
            .map(Membership::getRole)
            .filter(role -> role == GroupRole.OWNER || role == GroupRole.ADMIN)
            .isPresent();
    }

    /** Invitees with a PENDING or ACCEPTED invitation and active participants see the meetup (#28). */
    private boolean isInvitedOrParticipant(Long meetupId, String username) {
        return invitationRepository.existsByMeetupIdAndInviteeIdAndStatusIn(meetupId, username,
                EnumSet.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED))
            || participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(username, meetupId,
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING));
    }
}
