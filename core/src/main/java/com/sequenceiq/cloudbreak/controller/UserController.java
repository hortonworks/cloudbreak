package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.User;
import com.sequenceiq.cloudbreak.api.model.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.UserProfileResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Component
@Transactional(TxType.NEVER)
public class UserController implements UserEndpoint {

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserProfileService userProfileService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public String evictUserDetails(String id, User user) {
        userDetailsService.evictUserDetails(id, user.getUsername());
        return user.getUsername();
    }

    @Override
    public User evictCurrentUserDetails() {
        IdentityUser user = authenticatedUserService.getCbUser();
        userDetailsService.evictUserDetails(user.getUserId(), user.getUsername());
        return new User(user.getUsername());
    }

    @Override
    public UserProfileResponse getProfile() {
        IdentityUser user = authenticatedUserService.getCbUser();
        UserProfile userProfile = userProfileService.get(user.getAccount(), user.getUserId(), user.getUsername());
        return conversionService.convert(userProfile, UserProfileResponse.class);
    }

    @Override
    public void modifyProfile(UserProfileRequest userProfileRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        userProfileService.put(userProfileRequest, user);
    }

}
