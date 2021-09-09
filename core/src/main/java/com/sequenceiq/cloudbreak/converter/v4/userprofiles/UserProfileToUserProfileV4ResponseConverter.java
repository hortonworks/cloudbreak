package com.sequenceiq.cloudbreak.converter.v4.userprofiles;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.domain.UserProfile;

@Component
public class UserProfileToUserProfileV4ResponseConverter {

    public UserProfileV4Response convert(UserProfile entity) {
        UserProfileV4Response userProfileV4Response = new UserProfileV4Response();
        userProfileV4Response.setUsername(entity.getUserName());
        userProfileV4Response.setUserId(entity.getUser().getUserId());
        userProfileV4Response.setTenant(entity.getUser().getTenant().getName());
        return userProfileV4Response;
    }

}
