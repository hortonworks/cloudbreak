package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.api.UserEndpoint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.model.UserRequest;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserController implements UserEndpoint {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public String evictUserDetails(String id, UserRequest userRequest) {
        userDetailsService.evictUserDetails(id, userRequest.getUsername());
        return userRequest.getUsername();
    }

    @Override
    public Boolean hasResources(String id) {
        CbUser user = authenticatedUserService.getCbUser();
        boolean hasResources = userDetailsService.hasResources(user, id);
        return hasResources;
    }

}
