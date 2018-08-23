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
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class UserController implements UserEndpoint {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public String evictUserDetails(String id, UserJson user) {
        cachedUserDetailsService.evictUserDetails(id, user.getUsername());
        userService.evictUser(restRequestThreadLocalService.getIdentityUser());
        return user.getUsername();
    }

    @Override
    public UserJson evictCurrentUserDetails() {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
        cachedUserDetailsService.evictUserDetails(user.getUserId(), user.getUsername());
        userService.evictUser(user);
        return new UserJson(user.getUsername());
    }

    @Override
    public UserProfileResponse getProfile() {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        UserProfile userProfile = userProfileService.getOrCreate(identityUser.getAccount(), identityUser.getUserId(), identityUser.getUsername(), user);
        return conversionService.convert(userProfile, UserProfileResponse.class);
    }

    @Override
    public void modifyProfile(UserProfileRequest userProfileRequest) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        userProfileService.put(userProfileRequest, identityUser, user, organization);
    }

    @Override
    public SortedSet<UserResponseJson> getAll() {
        IdentityUser user = restRequestThreadLocalService.getIdentityUser();
        Set<UserResponseJson> userResponseJsons = userService.getAll(user).stream()
                .map(u -> conversionService.convert(u, UserResponseJson.class))
                .collect(Collectors.toSet());
        SortedSet<UserResponseJson> results = new TreeSet<>(new UserIdComparator());
        results.addAll(userResponseJsons);
        return results;
    }
}
