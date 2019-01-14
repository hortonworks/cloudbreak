package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests.UserProfileV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserEvictV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class UserProfileV4Controller implements UserProfileV4Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public UserProfileV4Response get(Long workspaceId) {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        return conversionService.convert(userProfile, UserProfileV4Response.class);
    }

    @Override
    public UserProfileV4Response modify(Long workspaceId, UserProfileV4Request userProfileV4Request) {
        UserProfile userProfile = userProfileService.putForLoggedInUser(userProfileV4Request, workspaceId);
        return conversionService.convert(userProfile, UserProfileV4Response.class);
    }

    @Override
    public UserEvictV4Response evictCurrentUserDetails(Long workspaceId) {
        return new UserEvictV4Response(userProfileService.evictCurrentUserDetailsForLoggedInUser());
    }
}
