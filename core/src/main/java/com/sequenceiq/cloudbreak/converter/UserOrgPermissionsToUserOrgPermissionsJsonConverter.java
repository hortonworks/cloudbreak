package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.UserOrgPermissionsJson;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;

@Component
public class UserOrgPermissionsToUserOrgPermissionsJsonConverter extends AbstractConversionServiceAwareConverter<UserOrgPermissions, UserOrgPermissionsJson> {
    @Override
    public UserOrgPermissionsJson convert(UserOrgPermissions userOrgPermissions) {
        UserOrgPermissionsJson json = new UserOrgPermissionsJson();
        json.setUserName(userOrgPermissions.getUser().getName());
        json.setEmail(userOrgPermissions.getUser().getEmail());
        json.setCompany(userOrgPermissions.getUser().getCompany());
        json.setPermissions(userOrgPermissions.getPermissionSet());
        return json;
    }
}
