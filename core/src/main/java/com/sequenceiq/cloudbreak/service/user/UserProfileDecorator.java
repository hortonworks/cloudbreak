package com.sequenceiq.cloudbreak.service.user;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class UserProfileDecorator {

    @Inject
    private EntitlementService entitlementService;

    public UserProfileV4Response decorate(UserProfileV4Response userProfileV4Response, String userCrn) {
        userProfileV4Response.setEntitlements(entitlementService.getEntitlements(userCrn));
        return userProfileV4Response;
    }
}
