package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.UserWorkspacePermissionsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;

@Component
public class UserWorkspacePermissionsToUserWorkspacePermissionsV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<UserWorkspacePermissions, UserWorkspacePermissionsV4Response> {

    @Override
    public UserWorkspacePermissionsV4Response convert(UserWorkspacePermissions userWorkspacePermissions) {
        UserWorkspacePermissionsV4Response json = new UserWorkspacePermissionsV4Response();
        json.setUserName(userWorkspacePermissions.getUser().getUserName());
        json.setUserId(userWorkspacePermissions.getUser().getUserId());
        json.setPermissions(userWorkspacePermissions.getPermissionSet());
        return json;
    }
}
