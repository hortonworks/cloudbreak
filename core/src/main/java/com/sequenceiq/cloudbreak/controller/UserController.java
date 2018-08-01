package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.users.UserJson;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileRequest;
import com.sequenceiq.cloudbreak.api.model.users.UserProfileResponse;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson;
import com.sequenceiq.cloudbreak.api.model.users.UserResponseJson.UserIdComparator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.UserProfile;
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
    public SortedSet<UserResponseJson> getAll() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<UserResponseJson> userResponseJsons = userService.getAll(user).stream()
                .map(u -> conversionService.convert(u, UserResponseJson.class))
                .collect(Collectors.toSet());
        SortedSet<UserResponseJson> results = new TreeSet<>(new UserIdComparator());
        results.addAll(userResponseJsons);
        return results;
    }
}
