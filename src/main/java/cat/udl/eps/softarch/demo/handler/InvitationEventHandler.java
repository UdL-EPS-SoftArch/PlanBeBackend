package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Component
@RepositoryEventHandler
public class InvitationEventHandler {
    final InvitationRepository invitationRepository;
    final UserRepository userRepository;

    public InvitationEventHandler(InvitationRepository invitationRepository, UserRepository userRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @HandleBeforeCreate
    public void handleInvitationPreCreate(Invitation invitation) {
        Meetup meetup = invitation.getMeetup();
        if (meetup == null || invitation.getInvitee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meetup and invitee are required");
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User inviter = userRepository.findById(currentUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (meetup.getOrganizer() == null || !meetup.getOrganizer().getId().equals(inviter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the meetup organizer can send invitations");
        }

        if (invitationRepository.existsByInviteeIdAndMeetupId(invitation.getInvitee().getId(), meetup.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already invited to this meetup");
        }

        invitation.setInvitedBy(inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreated(Instant.now());
    }
}
