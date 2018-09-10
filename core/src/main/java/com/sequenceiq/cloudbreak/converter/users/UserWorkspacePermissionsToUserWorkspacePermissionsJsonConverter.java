package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.UserWorkspacePermissionsJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;

@Component
public class UserWorkspacePermissionsToUserWorkspacePermissionsJsonConverter
        extends AbstractConversionServiceAwareConverter<UserWorkspacePermissions, UserWorkspacePermissionsJson> {

    @Override
    public UserWorkspacePermissionsJson convert(UserWorkspacePermissions userWorkspacePermissions) {
        UserWorkspacePermissionsJson json = new UserWorkspacePermissionsJson();
        json.setUserName(userWorkspacePermissions.getUser().getUserName());
        json.setUserId(userWorkspacePermissions.getUser().getUserId());
        json.setPermissions(userWorkspacePermissions.getPermissionSet());
        return json;
    }
}
