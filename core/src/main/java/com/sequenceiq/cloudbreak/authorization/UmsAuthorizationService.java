package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.util.Optional;
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
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceRightUtils;

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
        umsClient.assignResourceRole(user.getUserCrn(), workspace.getResourceCrn(), role.getCrn(user.getUserCrn()), getRequestId());
    }

    public void unassignResourceRoleFromUserInWorkspace(User user, Workspace workspace, WorkspaceRole role) {
        umsClient.unassignResourceRole(user.getUserCrn(), workspace.getResourceCrn(), role.getCrn(user.getUserCrn()), getRequestId());
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action, String unauthorizedMessage) {
        if (!umsClient.checkRight(user.getUserCrn(), user.getUserCrn(), WorkspaceRightUtils.getRight(resource, action),
                workspace.getResourceCrn(), getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    public boolean hasRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        return umsClient.checkRight(user.getUserCrn(), user.getUserCrn(),
                WorkspaceRightUtils.getRight(resource, action), workspace.getResourceCrn(), getRequestId());
    }

    public void checkRightOfUserForResource(User user, Workspace workspace, WorkspaceResource resource, ResourceAction action) {
        String unauthorizedMessage = format("You have no right to perform %s on %s. This requires PowerUser role. "
                        + "You can request access through IAM service from an administrator.",
                WorkspaceRightUtils.getRight(resource, action), resource.getReadableName());
        checkRightOfUserForResource(user, workspace, resource, action, unauthorizedMessage);
    }

    public Set<WorkspaceRole> getUserRolesInWorkspace(User user, Workspace workspace) {
        return umsClient.listResourceRoleAssigments(user.getUserCrn(), user.getUserCrn(), getRequestId()).stream()
                .filter(resourceAssignment -> StringUtils.equals(workspace.getResourceCrn(), resourceAssignment.getResourceCrn()))
                .map(resourceAssignment -> WorkspaceRole.getByUmsName(Crn.fromString(resourceAssignment.getResourceRoleCrn()).getResource()))
                .collect(Collectors.toSet());
    }

    public void removeResourceRolesOfUserInWorkspace(Set<User> users, Workspace workspace) {
        users.stream().forEach(user -> getUserRolesInWorkspace(user, workspace).stream()
                .forEach(role -> unassignResourceRoleFromUserInWorkspace(user, workspace, role)));
    }

    public void notifyAltusAboutResourceDeletion(User currentUser, Workspace workspace) {
        umsClient.notifyResourceDeleted(currentUser.getUserCrn(), workspace.getResourceCrn(), getRequestId());
    }

    public Set<User> getUsersOfWorkspace(User currentUser, Workspace workspace) {
        Set<String> userIds = umsClient.listAssigneesOfResource(currentUser.getUserCrn(), currentUser.getUserCrn(),
                workspace.getResourceCrn(), getRequestId()).stream().map(resourceAssignee -> Crn.fromString(resourceAssignee.getAssigneeCrn()).getResource())
                .collect(Collectors.toSet());
        return userService.getByUsersIds(userIds);
    }

    public Set<Workspace> getWorkspacesOfCurrentUser(User currentUser) {
        Set<String> workspaceCrns = umsClient.listResourceRoleAssigments(currentUser.getUserCrn(), currentUser.getUserCrn(), getRequestId()).stream()
                .filter(resourceAssignment -> Crn.ResourceType.WORKSPACE.equals(Crn.fromString(resourceAssignment.getResourceCrn()).getResourceType()))
                .map(resourceAssignment -> resourceAssignment.getResourceCrn())
                .collect(Collectors.toSet());
        return Sets.newHashSet(workspaceRepository.findAllByCrn(workspaceCrns));
    }

    private Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}
