package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class AltusAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    public void assignResourceRoleToUserInWorkspace(User user, Workspace workspace, WorkspaceRole role) {
        if (umsClient.isUmsUsable(user.getCrn())) {
            umsClient.assignResourceRole(user.getCrn(), workspace.getResourceCrn(), role.getCrn(user.getCrn()));
        }
    }

    public void unassignResourceRoleFromUserInWorkspace(User user, Workspace workspace, WorkspaceRole role) {
        if (umsClient.isUmsUsable(user.getCrn())) {
            umsClient.unassignResourceRole(user.getCrn(), workspace.getResourceCrn(), role.getCrn(user.getCrn()));
        }
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action, String unauthorizedMessage) {
        if (umsClient.isUmsUsable(user.getCrn())
                && !umsClient.checkRight(user.getCrn(), WorkspaceRightUtils.getRight(resource, action), workspace.getResourceCrn())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    public boolean hasRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        if (umsClient.isUmsUsable(user.getCrn())) {
            return umsClient.checkRight(user.getCrn(), WorkspaceRightUtils.getRight(resource, action), workspace.getResourceCrn());
        }
        return true;
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        String unauthorizedMessage = format("You have no right to perform %s on %s in workspace %s.", action.name().toLowerCase(),
                resource.getReadableName(), workspace.getName());
        checkRightOfUserForResource(user, workspace, resource, action, unauthorizedMessage);
    }

    public Set<WorkspaceRole> getUserRolesInWorkspace(User user, Workspace workspace) {
        if (umsClient.isUmsUsable(user.getCrn())) {
            return umsClient.listResourceRoleAssigments(user.getCrn()).stream()
                    .filter(resourceAssignment -> StringUtils.equals(workspace.getResourceCrn(), resourceAssignment.getResourceCrn()))
                    .map(resourceAssignment -> WorkspaceRole.getByUmsName(Crn.fromString(resourceAssignment.getResourceRoleCrn()).getResource()))
                    .collect(Collectors.toSet());
        }
        return Sets.newHashSet(WorkspaceRole.WORKSPACEMANAGER);
    }

    public void removeResourceRolesOfUserInWorkspace(Set<User> users, Workspace workspace) {
        users.stream().forEach(user -> getUserRolesInWorkspace(user, workspace).stream()
                .forEach(role -> unassignResourceRoleFromUserInWorkspace(user, workspace, role)));
    }

    public void notifyAltusAboutResourceDeletion(User currentUser, Workspace workspace) {
        if (umsClient.isUmsUsable(currentUser.getCrn())) {
            umsClient.notifyResourceDeleted(currentUser.getCrn(), workspace.getResourceCrn());
        }
    }

    public Set<User> getUsersOfWorkspace(User currentUser, Workspace workspace) {
        if (umsClient.isUmsUsable(currentUser.getCrn())) {
            Set<String> userIds = umsClient.listAssigneesOfResource(currentUser.getCrn(), workspace.getResourceCrn()).stream()
                    .map(resourceAssignee -> Crn.fromString(resourceAssignee.getAssigneeCrn()).getResource())
                    .collect(Collectors.toSet());
            return userService.getByUsersIds(userIds);
        }
        return Sets.newHashSet(currentUser);
    }

    public Set<Workspace> getWorkspacesOfCurrentUser(User currentUser) {
        if (umsClient.isUmsUsable(currentUser.getCrn())) {
            Set<String> workspaceCrns = umsClient.listResourceRoleAssigments(currentUser.getCrn()).stream()
                    .filter(resourceAssignment -> Crn.ResourceType.WORKSPACE.equals(Crn.fromString(resourceAssignment.getResourceCrn()).getResourceType()))
                    .map(resourceAssignment -> resourceAssignment.getResourceCrn())
                    .collect(Collectors.toSet());
            return Sets.newHashSet(workspaceRepository.findAllByCrn(workspaceCrns));
        }
        return Sets.newHashSet(workspaceRepository.getByName(currentUser.getUserName(), currentUser.getTenant()));
    }
}
