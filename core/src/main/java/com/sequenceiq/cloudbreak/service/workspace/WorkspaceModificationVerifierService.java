package com.sequenceiq.cloudbreak.service.workspace;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.WORKSPACE_MANAGE;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.users.ChangeWorkspaceUsersJson;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions;
import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@Service
public class WorkspaceModificationVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceModificationVerifierService.class);

    @Inject
    private StackService stackService;

    @Inject
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    public void authorizeWorkspaceManipulation(User currentUser, Workspace workspaceToManipulate, Action action, String unautorizedMessage) {
        UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserAndWorkspace(currentUser, workspaceToManipulate);
        if (userWorkspacePermissions == null) {
            throw new AccessDeniedException("You have no access for this workspace.");
        }
        boolean hasPermission = WorkspacePermissions.hasPermission(userWorkspacePermissions.getPermissionSet(), WorkspaceResource.WORKSPACE, action);
        if (!hasPermission) {
            throw new AccessDeniedException(unautorizedMessage);
        }
    }

    public Set<UserWorkspacePermissions> validateAllUsersAreAlreadyInTheWorkspace(Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);
        Set<String> usersNotInTheWorkspace = new TreeSet<>();

        Set<UserWorkspacePermissions> userWorkspacePermissionsSet = users.stream()
                .map(user -> {
                    UserWorkspacePermissions userWorkspacePermissions = userWorkspacePermissionsService.findForUserAndWorkspace(user, workspace);
                    if (userWorkspacePermissions == null) {
                        usersNotInTheWorkspace.add(user.getUserId());
                    }
                    return userWorkspacePermissions;
                })
                .collect(Collectors.toSet());

        if (!usersNotInTheWorkspace.isEmpty()) {
            String usersCommaSeparated = String.join(", ", usersNotInTheWorkspace);
            throw new BadRequestException("The following users are not in the workspace: " + usersCommaSeparated);
        }

        return userWorkspacePermissionsSet;
    }

    public void validateUsersAreNotInTheWorkspaceYet(Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);

        Set<String> usersInWorkspace = users.stream()
                .filter(user -> userWorkspacePermissionsService.findForUserAndWorkspace(user, workspace) != null)
                .map(User::getUserId)
                .collect(Collectors.toSet());

        if (!usersInWorkspace.isEmpty()) {
            String usersCommaSeparated = String.join(", ", usersInWorkspace);
            throw new BadRequestException("The following users are already in the workspace: " + usersCommaSeparated);
        }
    }

    public void validateAllUsersAreInTheTenant(Workspace workspace, Set<User> users) {
        boolean anyUserNotInTenantOfWorkspace = users.stream()
                .anyMatch(user -> !user.getTenant().equals(workspace.getTenant()));

        if (anyUserNotInTenantOfWorkspace) {
            throw new NotFoundException("User(s) not found in tenant.");
        }
    }

    public void ensureWorkspaceManagementForUserRemoval(Workspace workspace, Set<String> userIds) {
        Set<UserWorkspacePermissions> existingUserPermissions = userWorkspacePermissionsService.findForWorkspace(workspace);
        Set<String> usersWithManagePermission = existingUserPermissions.stream()
                .filter(it -> it.getPermissionSet().contains(WORKSPACE_MANAGE.value()))
                .map(it -> it.getUser().getUserId())
                .collect(Collectors.toSet());

        usersWithManagePermission.removeAll(userIds);
        if (usersWithManagePermission.isEmpty()) {
            throw new BadRequestException(String.format("You cannot remove every user with '%s' permissions.", WORKSPACE_MANAGE));
        }
    }

    public void ensureWorkspaceManagementForUserUpdates(Workspace workspace, Set<ChangeWorkspaceUsersJson> userUpdates) {
        Set<UserWorkspacePermissions> existingUserPermissions = userWorkspacePermissionsService.findForWorkspace(workspace);
        Set<String> usersWithManagePermission = existingUserPermissions.stream()
                .filter(it -> it.getPermissionSet().contains(WORKSPACE_MANAGE.value()))
                .map(it -> it.getUser().getUserId())
                .collect(Collectors.toSet());
        Set<String> updateUserIds = userUpdates.stream().map(ChangeWorkspaceUsersJson::getUserId).collect(Collectors.toSet());

        usersWithManagePermission.removeAll(updateUserIds);
        if (usersWithManagePermission.isEmpty()) {
            if (userUpdates.stream().noneMatch(userUpdate -> userUpdate.getPermissions().contains(WORKSPACE_MANAGE.value()))) {
                throw new BadRequestException(String.format("No user with '%s' permission would remain in the workspace, "
                        + "therefore user update cannot be executed.", WORKSPACE_MANAGE));
            }
        }
    }

    public void ensureWorkspaceManagementForChangeUsers(Set<ChangeWorkspaceUsersJson> usersPermissions) {
        if (usersPermissions.stream().noneMatch(userPermissions -> userPermissions.getPermissions().contains(WORKSPACE_MANAGE.value()))) {
            throw new BadRequestException(String.format("No new user would have '%s' permission after user change operation, "
                    + "therefore it cannot be executed.", WORKSPACE_MANAGE));
        }
    }

    public void verifyDefaultWorkspaceUserUpdates(User initiator, Workspace workspace, Set<User> usersToBeUpdated) {
        verifyOperationRegardingDefaultWorkspaces(initiator, workspace, usersToBeUpdated,
                "You cannot change your permissions in your default workspace.",
                "You cannot modify the permission of %s in their default workspace.");
    }

    public void verifyDefaultWorkspaceUserRemovals(User initiator, Workspace workspace, Set<User> usersToBeRemoved) {
        verifyOperationRegardingDefaultWorkspaces(initiator, workspace, usersToBeRemoved,
                "You cannot remove yourself from your default workspace.",
                "You cannot remove %s from their default workspace.");
    }

    private void verifyOperationRegardingDefaultWorkspaces(User initiator, Workspace workspace, Set<User> usersToBeModified,
            String initiatorErrorMessage, String otherUserErrorMessage) {

        if (usersToBeModified.contains(initiator) && isDefaultWorkspaceForUser(workspace, initiator)) {
            throw new BadRequestException(initiatorErrorMessage);
        }

        Optional<User> usersDefaultWorkspace = usersToBeModified.stream()
                .filter(user -> isDefaultWorkspaceForUser(workspace, user))
                .findFirst();

        if (usersDefaultWorkspace.isPresent()) {
            throw new BadRequestException(String.format(otherUserErrorMessage, usersDefaultWorkspace.get().getUserId()));
        }
    }

    public boolean isDefaultWorkspaceForUser(Workspace workspace, User user) {
        return workspace.getName().equals(user.getUserId());
    }

    public void checkThatWorkspaceIsDeletable(User userWhoRequestTheDeletion, Workspace workspaceForDelete,
            Workspace defaultWorkspaceOfUserWhoRequestTheDeletion) {

        if (defaultWorkspaceOfUserWhoRequestTheDeletion.equals(workspaceForDelete)) {
            LOGGER.info("The requested {} workspace for delete is the same as the default workspace of the user {}.",
                    workspaceForDelete.getName(), userWhoRequestTheDeletion.getUserName());
            throw new BadRequestException(String.format("The following workspace '%s' could not deleted because this is your default workspace.",
                    workspaceForDelete.getName()));
        }
        if (stackService.anyStackInWorkspace(workspaceForDelete.getId())) {
            LOGGER.info("The requested {} workspace has already existing clusters. We can not delete them until those will be deleted",
                    workspaceForDelete.getName());
            throw new BadRequestException(String.format("The requested '%s' workspace has already existing clusters. "
                    + "Please delete them before you delete the workspace.", workspaceForDelete.getName()));
        }
    }
}
