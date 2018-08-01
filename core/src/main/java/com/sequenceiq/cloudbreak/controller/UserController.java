package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.users.UserJson;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class UserController implements UserEndpoint {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private UserService userService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public String evictUserDetails(String id, UserJson user) {
        cachedUserDetailsService.evictUserDetails(id, user.getUsername());
        return user.getUsername();
    }

    @Override
    public UserJson evictCurrentUserDetails() {
        IdentityUser user = authenticatedUserService.getCbUser();
        cachedUserDetailsService.evictUserDetails(user.getUserId(), user.getUsername());
        return new UserJson(user.getUsername());
    }

    @Override
    public UserProfileResponse getProfile() {
        IdentityUser user = authenticatedUserService.getCbUser();
        UserProfile userProfile = userProfileService.getOrCreate(user.getAccount(), user.getUserId(), user.getUsername());
        return conversionService.convert(userProfile, UserProfileResponse.class);
    }

    @Override
    public void modifyProfile(UserProfileRequest userProfileRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        userProfileService.put(userProfileRequest, user);
    }

    @Override
    public Set<UserResponseJson> getAll() {
        IdentityUser user = authenticatedUserService.getCbUser();
        return toJsonSet(userService.getAll(user));
    }

    private Set<UserResponseJson> toJsonSet(Set<User> users) {
        return (Set<UserResponseJson>) conversionService.convert(users, TypeDescriptor.forObject(users),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(UserJson.class)));
    }

}
