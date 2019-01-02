package com.sequenceiq.cloudbreak.converter.v4.workspaces;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.requests.UserWorkspacePermissionsV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;

@Component
public class UserWorkspacePermissionsToUserWorkspacePermissionsV4RequestConverter
        extends AbstractConversionServiceAwareConverter<UserWorkspacePermissions, UserWorkspacePermissionsV4Request> {

    @Override
    public UserWorkspacePermissionsV4Request convert(UserWorkspacePermissions userWorkspacePermissions) {
        UserWorkspacePermissionsV4Request json = new UserWorkspacePermissionsV4Request();
        json.setUserName(userWorkspacePermissions.getUser().getUserName());
        json.setUserId(userWorkspacePermissions.getUser().getUserId());
        json.setPermissions(userWorkspacePermissions.getPermissionSet());
        return json;
    }
}
