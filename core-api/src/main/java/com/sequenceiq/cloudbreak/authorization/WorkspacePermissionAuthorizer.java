package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.INVITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.MANAGE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.READ;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action.WRITE;
import static com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.getName;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.ALL;
import static com.sequenceiq.cloudbreak.authorization.WorkspaceResource.WORKSPACE;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;

@Component
public class WorkspacePermissionAuthorizer {

    public boolean hasPermission(Set<String> permissions, WorkspaceResource resource, Action action) {
        if (resource == WORKSPACE) {
            if (action == INVITE && (permissions.contains(getName(WORKSPACE, INVITE)) || permissions.contains(getName(WORKSPACE, MANAGE)))) {
                return true;
            }
            return action == MANAGE && permissions.contains(getName(WORKSPACE, MANAGE));
        } else if (permissions.contains(getName(ALL, WRITE))) {
            return true;
        } else if (permissions.contains(getName(ALL, READ)) && action == READ) {
            return true;
        }
        String permissionName = getName(resource, action);
        return permissions.contains(permissionName);
    }

}
