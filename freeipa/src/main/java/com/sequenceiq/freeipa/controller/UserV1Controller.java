package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersStatus;
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
        LOGGER.info("synchronizeUsers() request = {}", request);

        String accountId = "test_account";
        try {
            userService.synchronizeUsers(accountId, request);
        } catch (Exception e) {
            LOGGER.error("Failed to synchronizeUsers()", e);
            throw new RuntimeException(e);
        }

        return new SynchronizeUsersResponse("Hello synchronizeUsers()!");
    }

    @Override
    public SynchronizeUsersStatus getStatus() {
        LOGGER.info("getStatus()");
        return new SynchronizeUsersStatus("Hello getStatus()!");
    }

    @Override
    public SetPasswordResponse setPassword(String username, SetPasswordRequest request) {
        LOGGER.info("setPassword() requested for user {}", username);

        String accountId = "test_account";

        return passwordService.setPassword(accountId, username, request.getPassword());
    }
}
