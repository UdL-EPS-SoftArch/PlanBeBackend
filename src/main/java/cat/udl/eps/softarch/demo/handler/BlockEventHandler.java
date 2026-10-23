package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Block;
import cat.udl.eps.softarch.demo.domain.Invitation;
import cat.udl.eps.softarch.demo.domain.InvitationStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.BlockRepository;
import cat.udl.eps.softarch.demo.repository.InvitationRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;

/**
 * #48 Block users. The authenticated user is always the blocker. A block is never modified:
 * to unblock, the blocker (or an ADMIN) deletes it with DELETE /blocks/{id}.
 * What a block forbids is checked by ParticipationEventHandler (join) and InvitationEventHandler (invite).
 * Creating a block also takes the blocked user out of the blocker's meetups that have not ended yet.
 */
@Component
@RepositoryEventHandler
public class BlockEventHandler {
    final BlockRepository blockRepository;
    final UserRepository userRepository;
    final ParticipationRepository participationRepository;
    final InvitationRepository invitationRepository;
    final WaitingList waitingList;
    final TransactionTemplate transactionTemplate;

    public BlockEventHandler(BlockRepository blockRepository, UserRepository userRepository,
                             ParticipationRepository participationRepository, InvitationRepository invitationRepository,
                             WaitingList waitingList, TransactionTemplate transactionTemplate) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.invitationRepository = invitationRepository;
        this.waitingList = waitingList;
        this.transactionTemplate = transactionTemplate;
    }

    @HandleBeforeCreate
    public void handleBlockPreCreate(Block block) {
        if (block.getBlocked() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user to block is required");
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User blocker = userRepository.findById(currentUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (blocker.getId().equals(block.getBlocked().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), block.getBlocked().getId())) {
            // Retrying a block whose clean-up failed (e.g. a 5xx after the block was saved) finishes it
            removeFromBlockerMeetups(blocker.getId(), block.getBlocked().getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already blocked");
        }

        block.setBlocker(blocker);
        block.setCreated(Instant.now());
    }

    @HandleAfterCreate
    public void handleBlockPostCreate(Block block) {
        removeFromBlockerMeetups(block.getBlocker().getId(), block.getBlocked().getId());
    }

    /**
     * A blocked user cannot be in the blocker's meetups that have not ended (DRAFT, OPEN or FULL):
     * - their CONFIRMED or WAITING participations are cancelled, so the waiting list cannot promote them later.
     *   A confirmed spot freed this way goes to the next user waiting, as with any other cancellation;
     * - the blocker's PENDING or ACCEPTED invitations to them are DECLINED, so they can neither accept them
     *   nor keep seeing the private meetup through them (MeetupSecurity).
     * All of it in one transaction: either the user is completely out or nothing changed. It is idempotent.
     * Finished and cancelled meetups are history and are not touched.
     */
    private void removeFromBlockerMeetups(String blockerId, String blockedId) {
        EnumSet<MeetupStatus> notEnded = EnumSet.of(MeetupStatus.DRAFT, MeetupStatus.OPEN, MeetupStatus.FULL);
        transactionTemplate.executeWithoutResult(transaction -> {
            for (Participation participation : participationRepository
                    .findByParticipantIdAndMeetupOrganizerIdAndStatusInAndMeetupStatusIn(blockedId, blockerId,
                        EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING), notEnded)) {
                boolean wasConfirmed = participation.getStatus() == ParticipationStatus.CONFIRMED;
                participation.setStatus(ParticipationStatus.CANCELLED);
                participationRepository.save(participation);
                MeetupStatus meetupStatus = participation.getMeetup().getStatus();
                if (wasConfirmed && (meetupStatus == MeetupStatus.OPEN || meetupStatus == MeetupStatus.FULL)) {
                    waitingList.fillFreeSpots(participation.getMeetup().getId());
                }
            }
            for (Invitation invitation : invitationRepository
                    .findByInviteeIdAndMeetupOrganizerIdAndStatusInAndMeetupStatusIn(blockedId, blockerId,
                        EnumSet.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED), notEnded)) {
                invitation.setStatus(InvitationStatus.DECLINED);
                invitationRepository.save(invitation);
            }
        });
    }

    @HandleBeforeSave
    public void handleBlockPreSave(Block block) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A block cannot be modified: delete it to unblock the user");
    }

    @HandleBeforeLinkSave
    public void handleBlockPreLinkSave(Block block, Object linked) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A block cannot be modified: delete it to unblock the user");
    }

    @HandleBeforeLinkDelete
    public void handleBlockPreLinkDelete(Block block, Object linked) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A block cannot be modified: delete it to unblock the user");
    }

    /** Defence in depth: BlockRepository.findById already hides the blocks of other users (404). */
    @HandleBeforeDelete
    public void handleBlockPreDelete(Block block) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (block.getBlocker() == null || !block.getBlocker().getId().equals(authentication.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the blocker can remove a block");
        }
    }
}
