package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.enums.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.enums.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.ParticipationStatus;
import cat.udl.eps.softarch.demo.repository.BlockRepository;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;

@Component
@RepositoryEventHandler
public class InvitationEventHandler {
    final InvitationRepository invitationRepository;
    final UserRepository userRepository;
    final ParticipationRepository participationRepository;
    final WaitingList waitingList;
    final Notifier notifier;
    final BlockRepository blockRepository;

    public InvitationEventHandler(InvitationRepository invitationRepository, UserRepository userRepository,
        ParticipationRepository participationRepository, WaitingList waitingList, Notifier notifier,
        BlockRepository blockRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.waitingList = waitingList;
        this.notifier = notifier;
        this.blockRepository = blockRepository;
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

        // #48: a blocked user cannot invite the user who blocked them
        if (blockRepository.existsByBlockerIdAndBlockedId(invitation.getInvitee().getId(), inviter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot invite a user who has blocked you");
        }
        // ...and the blocker cannot send an invitation that the blocked user would never be allowed to accept
        if (blockRepository.existsByBlockerIdAndBlockedId(inviter.getId(), invitation.getInvitee().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot invite a user you have blocked");
        }

        if (isClosed(meetup)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot invite to a cancelled or finished meetup");
        }

        if (invitationRepository.existsByInviteeIdAndMeetupId(invitation.getInvitee().getId(), meetup.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already invited to this meetup");
        }

        invitation.setInvitedBy(inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreated(Instant.now());
    }

    /** #42: the invitee gets an INVITATION notification pointing to the invitation (to accept/reject it). */
    @HandleAfterCreate
    public void handleInvitationPostCreate(Invitation invitation) {
        User inviter = invitation.getInvitedBy();
        String inviterName = inviter.getDisplayName() != null && !inviter.getDisplayName().isBlank()
            ? inviter.getDisplayName() : inviter.getId();
        notifier.notify(invitation.getInvitee(), NotificationType.INVITATION,
            "%s invited you to the meetup \"%s\"".formatted(inviterName, invitation.getMeetup().getTitle()),
            invitation);
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

        // #48: blocked after being invited: accepting would join the blocker's meetup
        User organizer = invitation.getMeetup().getOrganizer();
        if (invitation.getStatus() == InvitationStatus.ACCEPTED && organizer != null
                && blockRepository.existsByBlockerIdAndBlockedId(organizer.getId(), currentUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The organizer of this meetup has blocked you");
        }

        if (invitation.getStatus() == InvitationStatus.ACCEPTED && isClosed(invitation.getMeetup())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot accept an invitation to a cancelled or finished meetup");
        }
    }

    @HandleAfterSave
    public void handleInvitationPostSave(Invitation invitation) {
        if (invitation.getStatus() != InvitationStatus.ACCEPTED) {
            return;
        }

        // Accepting joins the meetup like a regular join: the invitee enters the queue and WaitingList
        // gives them a spot if there is one (and sets the meetup OPEN/FULL)
        Meetup meetup = invitation.getMeetup();
        String inviteeId = invitation.getInvitee().getId();
        if (participationRepository.existsByParticipantIdAndMeetupIdAndStatusIn(inviteeId, meetup.getId(),
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING))) {
            return;
        }

        Participation participation = new Participation();
        participation.setParticipant(invitation.getInvitee());
        participation.setMeetup(meetup);
        participation.setStatus(ParticipationStatus.WAITING);
        participation.setJoinedAt(Instant.now());
        participationRepository.save(participation);
        // #48: blocked by the organizer while accepting (after the check in handleInvitationPreSave)
        if (meetup.getOrganizer() != null
                && blockRepository.existsByBlockerIdAndBlockedId(meetup.getOrganizer().getId(), inviteeId)) {
            participation.setStatus(ParticipationStatus.CANCELLED);
            participationRepository.save(participation);
            return;
        }
        waitingList.fillFreeSpots(meetup.getId(), inviteeId);
    }

    private boolean isClosed(Meetup meetup) {
        return meetup.getStatus() == MeetupStatus.CANCELLED || meetup.getStatus() == MeetupStatus.FINISHED;
    }
}
