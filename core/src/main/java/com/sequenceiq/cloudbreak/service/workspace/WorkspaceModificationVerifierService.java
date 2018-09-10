package com.sequenceiq.cloudbreak.service.workspace;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class WorkspaceModificationVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceModificationVerifierService.class);

    @Inject
    private StackService stackService;

    public void verifyUserUpdates(User initiator, Workspace workspace, Set<User> usersToBeUpdated) {
        verifyOperationRegardingDefaultWorkspaces(initiator, workspace, usersToBeUpdated,
                "You cannot change your permissions in your default workspace.",
                "You cannot modify the permission of %s in their default workspace.");
    }

    public void verifyRemovalFromDefaultWorkspace(User initiator, Workspace workspace, Set<User> usersToBeRemoved) {
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
        if (!stackService.findAllForWorkspace(workspaceForDelete.getId()).isEmpty()) {
            LOGGER.info("The requested {} workspace has already existing clusters. We can not delete them until those will be deleted",
                    workspaceForDelete.getName());
            throw new BadRequestException(String.format("The requested '%s' workspace has already existing clusters. "
                    + "Please delete them before you delete the workspace.", workspaceForDelete.getName()));
        }
    }
}
