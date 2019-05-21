package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.user.UsersyncV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersStatus;
import com.sequenceiq.freeipa.service.user.UsersyncService;

@Controller
public class UsersyncV1Controller implements UsersyncV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersyncV1Controller.class);

    @Inject
    private UsersyncService usersyncService;

    @Override
    public SynchronizeUsersResponse synchronizeUsers(SynchronizeUsersRequest request) {
        LOGGER.info("synchronizeUsers() request = {}", request);

        String accountId = "test_account";
        try {
            usersyncService.synchronizeUsers(accountId, request);
        } catch (Exception e) {
            LOGGER.error("failed to synchronizeUsers()", e);
            throw new RuntimeException(e);
        }

        return new SynchronizeUsersResponse("Hello synchronizeUsers()!");
    }

    @Override
    public SynchronizeUsersStatus getStatus() {
        LOGGER.info("getStatus()");
        return new SynchronizeUsersStatus("Hello getStatus()!");
    }
}
