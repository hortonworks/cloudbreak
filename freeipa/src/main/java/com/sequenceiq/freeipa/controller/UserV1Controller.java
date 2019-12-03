package com.sequenceiq.freeipa.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;

@Controller
public class UserV1Controller implements UserV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserV1Controller.class);

    @Inject
    private UserService userService;

    @Inject
    private PasswordService passwordService;

    @Inject
    private OperationStatusService operationStatusService;

    @Override
    public SyncOperationStatus synchronizeUser(SynchronizeUserRequest request) {
        String userCrn = checkUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("synchronizeUser() requested for user {} in account {}", userCrn, accountId);
        Set<String> environmentCrnFilter = request == null ? Set.of() : nullToEmpty(request.getEnvironments());
        Set<String> userCrnFilter = Set.of();
        Set<String> machineUserCrnFilter = Set.of();
        Crn crn = Crn.safeFromString(userCrn);
        switch (crn.getResourceType()) {
            case USER:
                userCrnFilter = Set.of(userCrn);
                break;
            case MACHINE_USER:
                machineUserCrnFilter = Set.of(userCrn);
                break;
            default:
                throw new BadRequestException(String.format("UserCrn %s is not of resoure type USER or MACHINE_USER", userCrn));
        }
        return checkOperationRejected(userService.synchronizeUsers(accountId, userCrn, environmentCrnFilter,
                userCrnFilter, machineUserCrnFilter));
    }

    @Override
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("synchronizeAllUsers() requested for account {}", accountId);

        return checkOperationRejected(userService.synchronizeUsers(accountId, userCrn, nullToEmpty(request.getEnvironments()),
                nullToEmpty(request.getUsers()), nullToEmpty(request.getMachineUsers())));
    }

    @Override
    public SyncOperationStatus setPassword(SetPasswordRequest request) {
        String userCrn = checkUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("setPassword() requested for user {} in account {}", userCrn, accountId);

        return checkOperationRejected(passwordService.setPassword(accountId, userCrn, userCrn, request.getPassword(),
                nullToEmpty(request.getEnvironments())));
    }

    @Override
    public SyncOperationStatus getSyncOperationStatus(@NotNull String operationId) {
        LOGGER.debug("getSyncOperationStatus() requested for operation {}", operationId);
        return operationStatusService.getSyncOperationStatus(operationId);
    }

    private SyncOperationStatus checkOperationRejected(SyncOperationStatus syncOperationStatus) {
        if (syncOperationStatus.getStatus() == SynchronizationStatus.REJECTED) {
            throw new SyncOperationAlreadyRunningException(syncOperationStatus.getError());
        }
        return syncOperationStatus;
    }

    private String checkUserCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("User CRN must be provided");
        }
        return userCrn;
    }

    private Set<String> nullToEmpty(Set<String> set) {
        return set == null ? Set.of() : set;
    }
}
