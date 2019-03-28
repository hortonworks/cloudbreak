package com.sequenceiq.cloudbreak.service.workspace;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.ChangeWorkspaceUsersV4Request;
import com.sequenceiq.cloudbreak.authorization.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.authorization.ResourceAction;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.authorization.WorkspaceRole;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class WorkspaceModificationVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceModificationVerifierService.class);

    @Inject
    private StackService stackService;

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

    public void authorizeWorkspaceManipulation(User currentUser, Workspace workspaceToManipulate, ResourceAction action, String unauthorizedMessage) {
        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspaceToManipulate);
        Optional<User> userInWorkspace = usersInWorkspace.stream().filter(user -> user.equals(currentUser)).findFirst();
        if (!userInWorkspace.isPresent()) {
            throw new AccessDeniedException("You have no access for this workspace.");
        }
        umsAuthorizationService.checkRightOfUserForResource(currentUser, workspaceToManipulate, WorkspaceResource.WORKSPACE, action, unauthorizedMessage);
    }

    public void validateAllUsersAreAlreadyInTheWorkspace(User currentUser, Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);

        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspace);
        if (!usersInWorkspace.containsAll(users)) {
            Set<String> usersNotPresentInWorkspace = users.stream()
                    .filter(user -> !usersInWorkspace.contains(user))
                    .map(user -> user.getUserName())
                    .collect(Collectors.toSet());
            String usersCommaSeparated = String.join(", ", usersNotPresentInWorkspace);
            throw new BadRequestException("The following users are not in the workspace: " + usersCommaSeparated);
        }
    }

    public void validateUsersAreNotInTheWorkspaceYet(User currentUser, Workspace workspace, Set<User> users) {
        validateAllUsersAreInTheTenant(workspace, users);

        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspace);
        Set<User> usersPresentInWorkspace = usersInWorkspace.stream().filter(user -> users.contains(user)).collect(Collectors.toSet());
        if (!usersPresentInWorkspace.isEmpty()) {
            String usersCommaSeparated = String.join(", ", usersPresentInWorkspace.stream().map(user -> user.getUserName()).collect(Collectors.toSet()));
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

    public void ensureWorkspaceManagementForUserRemainingUsers(User currentUser, Workspace workspace, Set<String> affectedUserIds) {
        Set<User> usersInWorkspace = umsAuthorizationService.getUsersOfWorkspace(currentUser, workspace);
        boolean remainedUserWithManagerRole = usersInWorkspace.stream()
                .filter(user -> !affectedUserIds.contains(user.getUserId()))
                .filter(user -> umsAuthorizationService.getUserRolesInWorkspace(user, workspace).stream()
                    .filter(role -> WorkspaceRole.WORKSPACEMANAGER.equals(role))
                    .count() > 0)
                .findAny()
                .isPresent();
        if (!remainedUserWithManagerRole) {
            throw new BadRequestException(String.format("You cannot remove every user with '%s' role.", WorkspaceRole.WORKSPACEMANAGER.getUmsName()));
        }
    }

    public void ensureWorkspaceManagementForUserUpdates(User currentUser, Workspace workspace, Set<ChangeWorkspaceUsersV4Request> userUpdates) {
        boolean updateHasUserWithManagerRole = userUpdates.stream()
                .filter(update -> update.getRoles() != null && update.getRoles().contains(WorkspaceRole.WORKSPACEMANAGER))
                .findAny()
                .isPresent();
        if (!updateHasUserWithManagerRole) {
            try {
                ensureWorkspaceManagementForUserRemainingUsers(currentUser, workspace, userUpdates.stream()
                        .map(update -> update.getUserId())
                        .collect(Collectors.toSet()));
            } catch (BadRequestException e) {
                throw new BadRequestException(String.format("No user with '%s' role would remain in the workspace, "
                        + "therefore user update cannot be executed.", WorkspaceRole.WORKSPACEMANAGER.getUmsName()));
            }
        }
    }

    public void ensureWorkspaceManagementForChangeUsers(Set<ChangeWorkspaceUsersV4Request> usersPermissions) {
        if (usersPermissions.stream().noneMatch(userPermissions -> userPermissions.getRoles() != null
                && userPermissions.getRoles().contains(WorkspaceRole.WORKSPACEMANAGER))) {
            throw new BadRequestException(String.format("No new user would have '%s' role after user change operation, "
                    + "therefore it cannot be executed.", WorkspaceRole.WORKSPACEMANAGER.getUmsName()));
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
        return StringUtils.equals(workspace.getName(), user.getUserName());
    }

    public void checkThatWorkspaceIsDeletable(User userWhoRequestTheDeletion, Workspace workspaceForDelete,
            Workspace defaultWorkspaceOfUserWhoRequestTheDeletion) {

        if (defaultWorkspaceOfUserWhoRequestTheDeletion.equals(workspaceForDelete)) {
            LOGGER.info("The requested {} workspace for delete is the same as the default workspace of the user {}.",
                    workspaceForDelete.getName(), userWhoRequestTheDeletion.getUserName());
            throw new BadRequestException(String.format("The following workspace '%s' could not deleted because this is your default workspace.",
                    workspaceForDelete.getName()));
        }
        if (!stackService.findAllForWorkspace(workspaceForDelete.getId()).isEmpty()) {
            LOGGER.info("The requested {} workspace has already existing clusters. We can not delete them until those will be deleted",
                    workspaceForDelete.getName());
            throw new BadRequestException(String.format("The requested '%s' workspace has already existing clusters. "
                    + "Please delete them before you delete the workspace.", workspaceForDelete.getName()));
        }
    }
}
