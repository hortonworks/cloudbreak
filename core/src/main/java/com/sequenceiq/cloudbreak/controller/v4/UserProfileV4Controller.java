package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class UserProfileV4Controller implements UserProfileV4Endpoint {

    @Inject
    private UserProfileService userProfileService;

    @Autowired
    private ConverterUtil converterUtil;

    @Override
    public UserProfileV4Response get() {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        return converterUtil.convert(userProfile, UserProfileV4Response.class);
    }

}
