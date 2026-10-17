package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Component
@RepositoryEventHandler
public class InvitationEventHandler {
    final InvitationRepository invitationRepository;
    final UserRepository userRepository;
    final ParticipationRepository participationRepository;
    final MeetupRepository meetupRepository;

    public InvitationEventHandler(InvitationRepository invitationRepository, UserRepository userRepository,
        ParticipationRepository participationRepository, MeetupRepository meetupRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.meetupRepository = meetupRepository;
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

    @HandleBeforeSave
    public void handleInvitationPreSave(Invitation invitation) {
        if (invitation.getInvitee() == null || invitation.getMeetup() == null
            || !invitationRepository.existsByIdAndInviteeIdAndMeetupId(
                invitation.getId(), invitation.getInvitee().getId(), invitation.getMeetup().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only the invitation status can be modified");
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!invitation.getInvitee().getId().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the invitee can answer the invitation");
        }

        if (invitationRepository.findStoredStatusById(invitation.getId()) != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already answered");
        }

        if (invitation.getStatus() != InvitationStatus.ACCEPTED && invitation.getStatus() != InvitationStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation must be ACCEPTED or DECLINED");
        }
    }

    @HandleAfterSave
    @Transactional
    public void handleInvitationPostSave(Invitation invitation) {
        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            return;
        }

        // Accepting joins the meetup, respecting its capacity like a regular join
        Meetup meetup = invitation.getMeetup();
        Participation participation = participationRepository
            .findByParticipantIdAndMeetupId(invitation.getInvitee().getId(), meetup.getId())
            .orElseGet(Participation::new);
        if (participation.getId() != null && participation.getStatus() != ParticipationStatus.CANCELLED) {
            return;
        }

        long confirmed = participationRepository.countByMeetupIdAndStatus(meetup.getId(), ParticipationStatus.CONFIRMED);
        participation.setParticipant(invitation.getInvitee());
        participation.setMeetup(meetup);
        participation.setStatus(confirmed >= meetup.getCapacity() ? ParticipationStatus.WAITING : ParticipationStatus.CONFIRMED);
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);

        if (confirmed + 1 >= meetup.getCapacity() && participation.getStatus() == ParticipationStatus.CONFIRMED) {
            meetup.setStatus(Meetup.MeetupStatus.FULL);
            meetupRepository.save(meetup);
        }
    }
}
