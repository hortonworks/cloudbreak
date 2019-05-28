package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersResponse;
import com.sequenceiq.freeipa.service.user.PasswordService;
import com.sequenceiq.freeipa.service.user.UserService;

@Controller
public class UserV1Controller implements UserV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserV1Controller.class);

    @Inject
    private UserService userService;

    @Inject
    private PasswordService passwordService;

    @Override
    public SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request) {
        return userService.synchronizeUsers(request);
    }

    @Override
    public SynchronizeUsersResponse getStatus(String syncId) {
        return userService.getSynchronizeUsersStatus(syncId);
    }

    @Override
    public SetPasswordResponse setPassword(String username, SetPasswordRequest request) {
        LOGGER.debug("setPassword() requested for user {}", username);

        return passwordService.setPassword(username, request.getPassword());
    }

    @Override
    public CreateUsersResponse createUsers(CreateUsersRequest request) {
        try {
            userService.createUsers(request);
        } catch (Exception e) {
            LOGGER.error("Failed to create users", e);
            throw new RuntimeException(e);
        }

        return new CreateUsersResponse("Hello createUsers()!");
    }
}
