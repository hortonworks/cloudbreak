package com.sequenceiq.cloudbreak.workspace.authorization;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.workspace.authorization.api.WorkspaceRole;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
public class UmsWorkspaceAuthorizationService extends UmsAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsWorkspaceAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public void assignResourceRoleToUserInWorkspace(User user, String workspaceCrn, WorkspaceRole role) {
        umsClient.assignResourceRole(user.getUserCrn(), workspaceCrn, role.getCrn(user.getUserCrn()), getRequestId());
    }

    public void unassignResourceRoleFromUserInWorkspace(User user, String workspaceCrn, WorkspaceRole role) {
        umsClient.unassignResourceRole(user.getUserCrn(), workspaceCrn, role.getCrn(user.getUserCrn()), getRequestId());
    }

    public Set<WorkspaceRole> getUserRolesInWorkspace(User user, String workspaceCrn) {
        return umsClient.listResourceRoleAssigments(user.getUserCrn(), user.getUserCrn(), getRequestId()).stream()
                .filter(resourceAssignment -> StringUtils.equals(workspaceCrn, resourceAssignment.getResourceCrn()))
                .map(resourceAssignment -> WorkspaceRole.getByUmsName(Crn.fromString(resourceAssignment.getResourceRoleCrn()).getResource()))
                .collect(Collectors.toSet());
    }

    public void removeResourceRolesOfUserInWorkspace(Set<User> users, String workspaceCrn) {
        users.stream().forEach(user -> getUserRolesInWorkspace(user, workspaceCrn).stream()
                .forEach(role -> unassignResourceRoleFromUserInWorkspace(user, workspaceCrn, role)));
    }

    public void notifyAltusAboutResourceDeletion(User currentUser, String workspaceCrn) {
        umsClient.notifyResourceDeleted(currentUser.getUserCrn(), workspaceCrn, getRequestId());
    }

    public Set<String> getUserIdsOfWorkspace(User currentUser, String workspaceCrn) {
        Set<String> userIds = umsClient.listAssigneesOfResource(currentUser.getUserCrn(),
                    currentUser.getUserCrn(), workspaceCrn, getRequestId())
                .stream()
                .map(resourceAssignee -> Crn.fromString(resourceAssignee.getAssigneeCrn()).getResource())
                .collect(Collectors.toSet());
        return userIds;
    }
}
