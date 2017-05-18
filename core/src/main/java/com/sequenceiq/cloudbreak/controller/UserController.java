package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.User;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserResourceCheck;

@Component
public class UserController implements UserEndpoint {

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private UserResourceCheck userResourceCheck;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

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
    public Boolean hasResources(String id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return userResourceCheck.hasResources(user, id);
    }

}
