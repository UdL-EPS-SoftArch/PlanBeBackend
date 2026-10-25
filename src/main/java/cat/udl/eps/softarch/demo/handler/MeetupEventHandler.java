package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.GroupRole;
import cat.udl.eps.softarch.demo.domain.Meetup;
import cat.udl.eps.softarch.demo.domain.Meetup.MeetupStatus;
import cat.udl.eps.softarch.demo.domain.Meetup.Visibility;
import cat.udl.eps.softarch.demo.domain.Membership;
import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import cat.udl.eps.softarch.demo.domain.Participation;
import cat.udl.eps.softarch.demo.domain.ParticipationStatus;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.repository.MeetupRepository;
import cat.udl.eps.softarch.demo.repository.GroupRepository;
import cat.udl.eps.softarch.demo.repository.MembershipRepository;
import cat.udl.eps.softarch.demo.repository.ParticipationRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * #43: notifies participants (CONFIRMED and WAITING) when the organizer cancels a meetup, reopens a
 * cancelled one, or changes its date or location. #44: a bigger capacity or (re)opening the meetup
 * promotes the waiting list (SPOT_FREED).
 *
 * Changes are detected against the state loaded from the database (Meetup#loadedState), never against
 * what the client claims. Only the organizer (or a platform ADMIN, or an admin of the group) may modify a
 * meetup, as in the #23 branch, so nobody else can make the system send notifications to its participants.
 *
 * RF-5.4 (issue #33): a GROUP meetup belongs to one group, is created and managed by the owner/admins of
 * that group, and keeps its group forever.
 */
@Component
@RepositoryEventHandler
public class MeetupEventHandler {

    private final ParticipationRepository participationRepository;
    private final Notifier notifier;
    private final WaitingList waitingList;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
          private final RecurringMeetups recurringMeetups;
    private final MeetupRepository meetupRepository;

    public MeetupEventHandler(ParticipationRepository participationRepository, Notifier notifier,
                              WaitingList waitingList, UserRepository userRepository,
                              GroupRepository groupRepository, MembershipRepository membershipRepository, RecurringMeetups recurringMeetups, MeetupRepository meetupRepository) {
        this.participationRepository = participationRepository;
        this.notifier = notifier;
        this.waitingList = waitingList;
        this.userRepository = userRepository;
        this.recurringMeetups = recurringMeetups;
        this.meetupRepository = meetupRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    @HandleBeforeCreate
    public void handleMeetupPreCreate(Meetup meetup) {
        // The organizer is always the authenticated user, whatever the request body says.
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User organizer = userRepository.findById(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        meetup.setOrganizer(organizer);
        if (meetup.getStartDate() != null && !meetup.getStartDate().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be a future date");
        }
        // #53: only RecurringMeetups links occurrences
        meetup.setPreviousOccurrence(null);
        // RF-5.4: only the group owner or one of its admins creates a meetup for the group.
        checkGroupRules(meetup, SecurityContextHolder.getContext().getAuthentication());
    }

    @HandleBeforeSave
    public void handleMeetupPreSave(Meetup meetup) {
        checkOrganizerUnchangedAndCanModify(meetup);
        Long previousId = meetup.getPreviousOccurrence() == null ? null : meetup.getPreviousOccurrence().getId();
        if (!Objects.equals(meetupRepository.findStoredPreviousOccurrenceId(meetup.getId()), previousId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The previous occurrence cannot be changed");
        }
    }

    @HandleBeforeLinkSave
    public void handleMeetupPreLinkSave(Meetup meetup, Object linked) {
        checkOrganizerUnchangedAndCanModify(meetup);
        if (linked instanceof Meetup) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The previous occurrence cannot be changed");
        }
    }

    @HandleBeforeLinkDelete
    public void handleMeetupPreLinkDelete(Meetup meetup, Object linked) {
        checkOrganizerUnchangedAndCanModify(meetup);
    }

    @HandleBeforeDelete
    public void handleMeetupPreDelete(Meetup meetup) {
        checkOrganizerUnchangedAndCanModify(meetup);
    }

    @HandleAfterSave
    @Transactional
    public void handleMeetupPostSave(Meetup meetup) {
        Meetup.LoadedState before = requireLoadedState(meetup);
        String organizerId = before.organizerId();
        boolean wasCancelled = before.status() == MeetupStatus.CANCELLED;
        boolean isCancelled = meetup.getStatus() == MeetupStatus.CANCELLED;

        if (!wasCancelled && isCancelled) {
            notifier.notifyAll(activeParticipants(meetup), organizerId, NotificationType.MEETUP_CANCELLED,
                "The meetup \"%s\" has been cancelled".formatted(meetup.getTitle()), meetup);
          
            participationRepository.findByMeetupIdAndStatusIn(
                meetup.getId(),
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING)
            ).forEach(participation -> {
                participation.setStatus(ParticipationStatus.CANCELLED);
                participationRepository.save(participation);
            });
          
        } else if (!isCancelled) {
            List<String> changes = new ArrayList<>();
            if (!Objects.equals(before.startDate(), meetup.getStartDate())
                || !Objects.equals(before.endDate(), meetup.getEndDate())) changes.add("date");
            if (!Objects.equals(before.location(), meetup.getLocation())) changes.add("location");
            String message = null;
            if (wasCancelled) {
                // Otherwise MEETUP_CANCELLED would stay as the participants' last (and now false) notice.
                message = "The meetup \"%s\" has been reopened".formatted(meetup.getTitle());
            } else if (!changes.isEmpty()) {
                message = "The %s of the meetup \"%s\" has changed"
                    .formatted(String.join(" and ", changes), meetup.getTitle());
            }
            if (message != null) {
                notifier.notifyAll(activeParticipants(meetup), organizerId, NotificationType.MEETUP_UPDATED,
                    message, meetup);
            }
        }
        // A bigger capacity frees spots, and (re)opening a meetup (DRAFT/CANCELLED -> OPEN, or OPEN/FULL set
        // by hand) may not match the counts: the waiting list is promoted (SPOT_FREED) and OPEN/FULL is
        // recalculated from the confirmed participants, whatever the client sent.
        boolean statusToOpenOrFull = meetup.getStatus() != before.status()
            && (meetup.getStatus() == MeetupStatus.OPEN || meetup.getStatus() == MeetupStatus.FULL);
        if (!Objects.equals(before.capacity(), meetup.getCapacity()) || statusToOpenOrFull) {
            waitingList.fillFreeSpots(meetup.getId());
        }
        // #53: finishing an occurrence of a recurring meetup creates the next one
        if (before.status() != MeetupStatus.FINISHED && meetup.getStatus() == MeetupStatus.FINISHED) {
            recurringMeetups.createNextOccurrence(meetup);
        }
        // Already notified: a second save of this same instance must not notify again.
        meetup.captureLoadedState();
    }

    private List<User> activeParticipants(Meetup meetup) {
        return participationRepository.findByMeetupIdAndStatusIn(meetup.getId(),
                EnumSet.of(ParticipationStatus.CONFIRMED, ParticipationStatus.WAITING))
            .stream().map(Participation::getParticipant).toList();
    }

    private void checkOrganizerUnchangedAndCanModify(Meetup meetup) {
        Meetup.LoadedState before = requireLoadedState(meetup);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        // The organizer and the group are immutable: the request body cannot be used to take over a meetup.
        String organizerId = meetup.getOrganizer() == null ? null : meetup.getOrganizer().getId();
        if (!Objects.equals(before.organizerId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The organizer of a meetup cannot be changed");
        }
        Long groupId = meetup.getGroup() == null ? null : meetup.getGroup().getId();
        if (!Objects.equals(before.groupId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The group of a meetup cannot be changed");
        }
        checkVisibilityMatchesGroup(meetup);
        // Use the organizer and the group stored in the DB: the request body may be trying to replace them.
        if (isAdmin
            || authentication.getName().equals(before.organizerId())
            || canManageGroup(authentication, before.groupId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can modify the meetup");
    }

    /**
     * RF-5.4: a GROUP meetup always belongs to one group and no other meetup belongs to a group.
     * Only the owner of that group and its admins may create and manage it.
     */
    private void checkGroupRules(Meetup meetup, Authentication authentication) {
        checkVisibilityMatchesGroup(meetup);
        if (meetup.getGroup() == null) {
            return;
        }
        Group group = groupRepository.findByIdRaw(meetup.getGroup().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        // Never trust the group instance sent in the request body.
        meetup.setGroup(group);
        if (!canManageGroup(authentication, group)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the group owner or an admin can create a group meetup");
        }
    }

    private static void checkVisibilityMatchesGroup(Meetup meetup) {
        boolean hasGroup = meetup.getGroup() != null;
        boolean isGroupMeetup = meetup.getVisibility() == Visibility.GROUP;
        if (isGroupMeetup && !hasGroup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A GROUP meetup must belong to a group");
        }
        if (hasGroup && !isGroupMeetup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A meetup that belongs to a group must have GROUP visibility");
        }
    }

    private boolean canManageGroup(Authentication authentication, Long groupId) {
        if (groupId == null) return false;
        return groupRepository.findByIdRaw(groupId)
            .map(group -> canManageGroup(authentication, group))
            .orElse(false);
    }

    private boolean canManageGroup(Authentication authentication, Group group) {
        if (group.getOwnedBy() != null && authentication.getName().equals(group.getOwnedBy().getId())) {
            return true;
        }
        return membershipRepository.findByGroupIdAndMemberId(group.getId(), authentication.getName())
            .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
            .map(Membership::getRole)
            .filter(role -> role == GroupRole.OWNER || role == GroupRole.ADMIN)
            .isPresent();
    }

    private static Meetup.LoadedState requireLoadedState(Meetup meetup) {
        Meetup.LoadedState before = meetup.loadedState();
        if (before == null) {
            // Never guess what changed: refuse loudly instead of silently skipping notifications.
            throw new IllegalStateException("Meetup " + meetup.getId() + " has no loaded state");
        }
        return before;
    }
}
