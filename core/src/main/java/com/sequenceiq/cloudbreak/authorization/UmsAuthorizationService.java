package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.util.Set;
import java.util.UUID;
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
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Service
public class UmsAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceRepository workspaceRepository;

    public void assignResourceRoleToUserInWorkspace(User user, Workspace workspace, WorkspaceRole role) {
        if (umsClient.isUmsUsable(user.getUserCrn())) {
            umsClient.assignResourceRole(user.getUserCrn(), workspace.getResourceCrn(), role.getCrn(user.getUserCrn()), getRequestId());
        }
    }

    public void unassignResourceRoleFromUserInWorkspace(User user, Workspace workspace, WorkspaceRole role) {
        if (umsClient.isUmsUsable(user.getUserCrn())) {
            umsClient.unassignResourceRole(user.getUserCrn(), workspace.getResourceCrn(), role.getCrn(user.getUserCrn()), getRequestId());
        }
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action, String unauthorizedMessage) {
        if (umsClient.isUmsUsable(user.getUserCrn())
                && !umsClient.checkRight(user.getUserCrn(), WorkspaceRightUtils.getRight(resource, action), workspace.getResourceCrn(), getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    public boolean hasRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        if (umsClient.isUmsUsable(user.getUserCrn())) {
            return umsClient.checkRight(user.getUserCrn(), WorkspaceRightUtils.getRight(resource, action), workspace.getResourceCrn(), getRequestId());
        }
        return true;
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        String unauthorizedMessage = format("You have no right to perform %s on %s in workspace %s.", action.name().toLowerCase(),
                resource.getReadableName(), workspace.getName());
        checkRightOfUserForResource(user, workspace, resource, action, unauthorizedMessage);
    }

    public Set<WorkspaceRole> getUserRolesInWorkspace(User user, Workspace workspace) {
        if (umsClient.isUmsUsable(user.getUserCrn())) {
            return umsClient.listResourceRoleAssigments(user.getUserCrn(), getRequestId()).stream()
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
        if (umsClient.isUmsUsable(currentUser.getUserCrn())) {
            umsClient.notifyResourceDeleted(currentUser.getUserCrn(), workspace.getResourceCrn(), getRequestId());
        }
    }

    public Set<User> getUsersOfWorkspace(User currentUser, Workspace workspace) {
        if (umsClient.isUmsUsable(currentUser.getUserCrn())) {
            Set<String> userIds = umsClient.listAssigneesOfResource(currentUser.getUserCrn(), workspace.getResourceCrn(), getRequestId()).stream()
                    .map(resourceAssignee -> Crn.fromString(resourceAssignee.getAssigneeCrn()).getResource())
                    .collect(Collectors.toSet());
            return userService.getByUsersIds(userIds);
        }
        return Sets.newHashSet(currentUser);
    }

    public Set<Workspace> getWorkspacesOfCurrentUser(User currentUser) {
        if (umsClient.isUmsUsable(currentUser.getUserCrn())) {
            Set<String> workspaceCrns = umsClient.listResourceRoleAssigments(currentUser.getUserCrn(), getRequestId()).stream()
                    .filter(resourceAssignment -> Crn.ResourceType.WORKSPACE.equals(Crn.fromString(resourceAssignment.getResourceCrn()).getResourceType()))
                    .map(resourceAssignment -> resourceAssignment.getResourceCrn())
                    .collect(Collectors.toSet());
            return Sets.newHashSet(workspaceRepository.findAllByCrn(workspaceCrns));
        }
        return Sets.newHashSet(workspaceRepository.getByName(Crn.fromString(currentUser.getUserCrn()).getAccountId(), currentUser.getTenant()));
    }

    private String getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
