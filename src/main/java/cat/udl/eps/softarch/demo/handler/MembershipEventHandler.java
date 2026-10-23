package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.GroupRole;
import cat.udl.eps.softarch.demo.domain.Membership;
import cat.udl.eps.softarch.demo.domain.enums.MembershipStatus;
import cat.udl.eps.softarch.demo.domain.enums.NotificationType;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.enums.Visibility;
import cat.udl.eps.softarch.demo.repository.MembershipRepository;
import cat.udl.eps.softarch.demo.repository.UserRepository;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RF-5.2 (Epics #29): group memberships with role OWNER / ADMIN / MEMBER. The OWNER of a group (or a platform
 * ADMIN) manages its memberships: they change the role of a member (promote to ADMIN) and expel members.
 * A MEMBER cannot change any role: only the owner or an admin of the group may manage memberships.
 *
 * RF-5.3 (Epics #29): a user can request to join a public group. The request creates a Membership with status
 * PENDING, and the owner/admins of the group approve it (PENDING -> ACTIVE). Managers add members directly (ACTIVE).
 */
@Component
@RepositoryEventHandler
public class MembershipEventHandler {

    final MembershipRepository membershipRepository;
    final UserRepository userRepository;
    final Notifier notifier;

    public MembershipEventHandler(MembershipRepository membershipRepository, UserRepository userRepository,
        Notifier notifier) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
    }

    @HandleBeforeCreate
    public void handleMembershipPreCreate(Membership membership) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        if (membership.getGroup() == null || membership.getGroup().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is required");
        }
        Group group = membership.getGroup();
        if (membership.getMember() == null || membership.getMember().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is required");
        }
        String memberId = membership.getMember().getId();
        boolean selfRequest = memberId.equals(authentication.getName());
        if (selfRequest && !canManageGroup(authentication, group, null)) {
            // RF-5.3: a user may only request to join a PUBLIC group, and the request awaits approval.
            // The role and the status are forced so that a client cannot grant itself membership rights.
            if (group.getVisibility() != Visibility.PUBLIC) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only public groups accept join requests");
            }
            membership.setRole(GroupRole.MEMBER);
            membership.setStatus(MembershipStatus.PENDING);
        } else {
            // Only the owner of the group and its admins may add a member to it, directly as ACTIVE.
            assertCanManageGroup(authentication, group, null);
            if (membership.getRole() == null) {
                membership.setRole(GroupRole.MEMBER);
            }
            if (membership.getRole() == GroupRole.OWNER && !isGroupOwner(authentication, group)) {
                // Handing over the ownership of the group is up to the current owner, not to an admin.
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can grant the OWNER role");
            }
            membership.setStatus(MembershipStatus.ACTIVE);
        }
        if (membershipRepository.findByGroupIdAndMemberId(group.getId(), memberId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The user is already a member of the group");
        }
        membership.setJoinedAt(Instant.now());
    }

    /**
     * RF-5.3: once a join request is stored, the owner and the admins of the group are notified (RF-10).
     * Notifications cannot be created earlier: they need the membership URI, so the request must be persisted.
     */
    @HandleAfterCreate
    public void handleMembershipPostCreate(Membership membership) {
        if (membership.getStatus() != MembershipStatus.PENDING) {
            return;
        }
        Group group = membership.getGroup();
        List<User> managers = new ArrayList<>();
        if (group.getOwnedBy() != null) {
            managers.add(group.getOwnedBy());
        }
        membershipRepository.findActiveManagersByGroupId(group.getId())
            .forEach(manager -> managers.add(manager.getMember()));
        notifier.notifyAll(managers, membership.getMember().getId(), NotificationType.GROUP_REQUEST,
            membership.getMember().getId() + " wants to join the group " + group.getName(), membership);
    }

    @HandleBeforeSave
    public void handleMembershipPreSave(Membership membership) {
        if (membership.getId() == null) {
            // New memberships get their member, role and status in the @HandleBeforeCreate defaults.
            return;
        }
        Membership.LoadedState before = requireLoadedState(membership);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        Group group = membership.getGroup();
        if (group == null || group.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is required");
        }
        // The member, the group and the join date are immutable. The status only moves from PENDING to ACTIVE,
        // which is how an OWNER or an ADMIN approves a join request (RF-5.3).
        boolean statusChanged = !Objects.equals(before.status(), membership.getStatus());
        boolean approvesRequest = before.status() == MembershipStatus.PENDING
            && membership.getStatus() == MembershipStatus.ACTIVE;
        if (!Objects.equals(before.groupId(), group.getId())
            || !Objects.equals(before.memberId(),
                membership.getMember() == null ? null : membership.getMember().getId())
            || !Objects.equals(before.joinedAt(), membership.getJoinedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only the role and the status of a membership can be modified");
        }
        if (statusChanged && !approvesRequest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The status of a membership can only change from PENDING to ACTIVE");
        }
        assertCanManageGroup(authentication, group, before);
        if (before.role() == membership.getRole()) {
            // Nothing to change: an update only carrying other fields is a no-op for the role.
            return;
        }
        if (membership.getRole() == GroupRole.OWNER && !isGroupOwner(authentication, group)) {
            // Handing over the ownership of the group is up to the current owner, not to an admin.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can grant the OWNER role");
        }
    }

    @HandleBeforeDelete
    public void handleMembershipPreDelete(Membership membership) {
        Membership.LoadedState before = requireLoadedState(membership);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        Group group = membership.getGroup();
        if (group == null || group.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group is required");
        }
        assertCanManageGroup(authentication, group, before);
    }

    @HandleAfterSave
    public void handleMembershipAfterSave(Membership membership) {
        membership.captureLoadedState();
    }

    /**
     * Only the owner of the group and its admins (a membership with the OWNER or the ADMIN role) manage the
     * memberships of that group. A plain member may manage none of them.
     */
    private void assertCanManageGroup(Authentication authentication, Group group, Membership.LoadedState before) {
        if (!canManageGroup(authentication, group, before)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the group owner or an admin can manage the group memberships");
        }
    }

    private boolean canManageGroup(Authentication authentication, Group group, Membership.LoadedState before) {
        if (isPlatformAdmin(authentication)) {
            return true;
        }
        if (isGroupOwner(authentication, group)) {
            // The owner of the group manages the memberships of the group.
            return true;
        }
        GroupRole actorRole = roleInGroup(group.getId(), authentication.getName(), before);
        return actorRole == GroupRole.ADMIN || actorRole == GroupRole.OWNER;
    }

    /**
     * The role of the acting user in the group, MEMBER when they do not belong to it.
     *
     * When the acting user is the member of the very row being changed, the persisted role is used: the
     * pending change is not applied yet, and a query would flush it and read the new role back (letting a
     * member promote itself). A PENDING membership grants no management rights either: approving it is
     * up to the owner or the admins, never to the requester.
     */
    private GroupRole roleInGroup(Long groupId, String username, Membership.LoadedState before) {
        if (before != null && Objects.equals(before.memberId(), username)) {
            return before.status() == MembershipStatus.ACTIVE ? before.role() : GroupRole.MEMBER;
        }
        return membershipRepository.findByGroupIdAndMemberId(groupId, username)
            .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
            .map(Membership::getRole)
            .orElse(GroupRole.MEMBER);
    }

    private boolean isGroupOwner(Authentication authentication, Group group) {
        return group.getOwnedBy() != null && group.getOwnedBy().getId().equals(authentication.getName());
    }

    private boolean isPlatformAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private static Membership.LoadedState requireLoadedState(Membership membership) {
        Membership.LoadedState before = membership.loadedState();
        if (before == null) {
            // Never guess what changed: refuse loudly instead of silently skipping role changes.
            throw new IllegalStateException("Membership " + membership.getId() + " has no loaded state");
        }
        return before;
    }
}
