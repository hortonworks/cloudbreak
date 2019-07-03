package com.sequenceiq.freeipa.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.SyncOperationStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;

@Controller
public class UserV1Controller implements UserV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserV1Controller.class);

    @Inject
    private UserService userService;

    @Inject
    private PasswordService passwordService;

    @Inject
    private SyncOperationStatusService syncOperationStatusService;

    @Inject
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Override
    public SyncOperationStatus synchronizeUser(SynchronizeUserRequest request) {
        String userCrn = checkUserCrn();
        LOGGER.debug("synchronizeUser() requested for user {}", userCrn);
        String accountId = threadBaseUserCrnProvider.getAccountId();

        return userService.synchronizeUser(accountId, userCrn, userCrn);
    }

    @Override
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkUserCrn();
        String accountId = threadBaseUserCrnProvider.getAccountId();
        return userService.synchronizeAllUsers(accountId, userCrn, request.getEnvironments(), request.getUsers());
    }

    @Override
    public SyncOperationStatus setPassword(SetPasswordRequest request) {
        String userCrn = checkUserCrn();
        LOGGER.debug("setPassword() requested for user {}", userCrn);
        String accountId = threadBaseUserCrnProvider.getAccountId();
        Set<String> envs = request.getEnvironments();
        return passwordService.setPassword(accountId, userCrn, request.getPassword(), envs);
    }

    @Override
    public SyncOperationStatus getSyncOperationStatus(@NotNull String operationId) {
        LOGGER.debug("getSyncOperationStatus() requested for operation {}", operationId);
        return syncOperationStatusService.getStatus(operationId);
    }

    private String checkUserCrn() {
        String userCrn = threadBaseUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("User CRN must be provided");
        }
        return userCrn;
    }
}
