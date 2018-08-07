package com.sequenceiq.cloudbreak.converter.users;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.UserOrgPermissionsJson;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;

@Component
public class UserOrgPermissionsToUserOrgPermissionsJsonConverter extends AbstractConversionServiceAwareConverter<UserOrgPermissions, UserOrgPermissionsJson> {
    @Override
    public UserOrgPermissionsJson convert(UserOrgPermissions userOrgPermissions) {
        UserOrgPermissionsJson json = new UserOrgPermissionsJson();
        json.setUserName(userOrgPermissions.getUser().getUserName());
        json.setUserId(userOrgPermissions.getUser().getUserId());
        json.setPermissions(userOrgPermissions.getPermissionSet());
        return json;
    }
}
