package cat.udl.eps.softarch.demo.handler;

import cat.udl.eps.softarch.demo.domain.Group;
import cat.udl.eps.softarch.demo.domain.User;
import cat.udl.eps.softarch.demo.domain.Enum.Visibility;
import cat.udl.eps.softarch.demo.repository.GroupRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class GroupEventHandler {
    final GroupRepository groupRepository;

    public GroupEventHandler(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @HandleBeforeCreate
    public void handleGroupPreCreate(Group group) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        group.setOwnedBy((User) authentication.getPrincipal());
        if (group.getVisibility() == null) {
            group.setVisibility(Visibility.PUBLIC);
        }
    }

    @HandleBeforeSave
    public void handleGroupPreSave(Group group) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User current = (User) authentication.getPrincipal();
        User persistedOwner = persistedOwner(group);

        if (persistedOwner == null) {
            // Group is not persisted yet (upsert): the requester becomes the owner
            group.setOwnedBy(current);
            return;
        }
        assertCanManage(authentication, persistedOwner, "edit");
        group.setOwnedBy(persistedOwner);
    }

    @HandleBeforeDelete
    public void handleGroupPreDelete(Group group) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertCanManage(authentication, persistedOwner(group), "delete");
    }

    private User persistedOwner(Group group) {
        if (group.getId() == null) {
            return null;
        }
        return groupRepository.findById(group.getId()).map(Group::getOwnedBy).orElse(null);
    }

    private void assertCanManage(Authentication authentication, User owner, String action) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        if (owner == null || !authentication.getName().equals(owner.getUsername())) {
            throw new AccessDeniedException("Only the owner or an admin can " + action + " the group");
        }
    }
}
