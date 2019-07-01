package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;
import com.sequenceiq.freeipa.util.CrnService;

import java.util.Set;

@Controller
public class UserV1Controller implements UserV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserV1Controller.class);

    @Inject
    private UserService userService;

    @Inject
    private PasswordService passwordService;

    @Inject
    private CrnService crnService;

    @Inject
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Override
    public SynchronizeUserResponse synchronizeUser(SynchronizeUserRequest request) {
        String userCrn = checkUserCrn();
        LOGGER.debug("synchronizeUser() requested for user {}", userCrn);
        String accountId = crnService.getCurrentAccountId();

        return userService.synchronizeUser(accountId, userCrn, userCrn);
    }

    @Override
    public SynchronizeAllUsersResponse synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkUserCrn();
        String accountId = crnService.getCurrentAccountId();
        return userService.synchronizeAllUsers(accountId, userCrn, request);
    }

    @Override
    public SynchronizeAllUsersResponse getSynchronizationStatus(String syncId) {
        return userService.getSynchronizeUsersStatus(syncId);
    }

    @Override
    public SetPasswordResponse setPassword(SetPasswordRequest request) {
        try {
            String userCrn = checkUserCrn();
            LOGGER.debug("setPassword() requested for user {}", userCrn);
            String accountId = crnService.getCurrentAccountId();
            Set<String> envs = request.getEnvironments();
            return passwordService.setPassword(accountId, userCrn, request.getPassword(), envs);
        } catch (Exception e) {
            LOGGER.error("setPassword caught exception. rethrowing", e);
            throw e;
        }
    }

    @Override
    public CreateUsersResponse createUsers(CreateUsersRequest request) {
        try {
            String accountId = crnService.getCurrentAccountId();
            userService.createUsers(accountId, request);
        } catch (Exception e) {
            LOGGER.error("Failed to create users", e);
            throw new RuntimeException(e);
        }

        return new CreateUsersResponse("Hello createUsers()!");
    }

    private String checkUserCrn() {
        String userCrn = threadBaseUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("User CRN must be provided");
        }
        return userCrn;
    }
}
