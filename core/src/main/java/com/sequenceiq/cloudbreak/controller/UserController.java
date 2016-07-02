package com.sequenceiq.cloudbreak.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.UserRequest;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

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
        return userDetailsService.hasResources(user, id);
    }

}
