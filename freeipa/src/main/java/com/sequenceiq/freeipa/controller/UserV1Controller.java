package com.sequenceiq.freeipa.controller;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Iterables;
import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;
import com.sequenceiq.freeipa.converter.freeipa.user.OperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.service.freeipa.user.EnvironmentUserSyncStateCalculator;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.PostUserSyncService;
import com.sequenceiq.freeipa.service.freeipa.user.PreUserSyncService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncRequestFilter;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Controller
public class UserV1Controller implements UserV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserV1Controller.class);

    @Inject
    private UserSyncService userSyncService;

    @Inject
    private PasswordService passwordService;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToSyncOperationStatus operationToSyncOperationStatus;

    @Inject
    private EnvironmentUserSyncStateCalculator environmentUserSyncStateCalculator;

    @Inject
    private PreUserSyncService preUserSyncService;

    @Inject
    private PostUserSyncService postUserSyncService;

    @Override
    @CustomPermissionCheck
    public SyncOperationStatus synchronizeUser(SynchronizeUserRequest request) {
        String userCrn = checkActorCrn();
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
        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(userCrnFilter, machineUserCrnFilter, Optional.empty());
        Operation syncOperation = userSyncService.synchronizeUsersWithCustomPermissionCheck(accountId, userCrn, environmentCrnFilter,
                userSyncFilter, WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return checkOperationRejected(operationToSyncOperationStatus.convert(syncOperation));
    }

    @Override
    @AccountIdNotNeeded
    @CustomPermissionCheck
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkActorCrn();
        String accountId = determineAccountId(userCrn, request.getAccountId());

        LOGGER.debug("synchronizeAllUsers() requested for account {}", accountId);

        UserSyncRequestFilter userSyncFilter = new UserSyncRequestFilter(nullToEmpty(request.getUsers()),
                nullToEmpty(request.getMachineUsers()),
                getOptionalDeletedWorkloadUser(request.getDeletedWorkloadUsers()));
        Operation syncOperation = userSyncService.synchronizeUsersWithCustomPermissionCheck(accountId, userCrn,
                nullToEmpty(request.getEnvironments()), userSyncFilter, request.getWorkloadCredentialsUpdateType(),
                AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return checkOperationRejected(operationToSyncOperationStatus.convert(syncOperation));
    }

    private Optional<String> getOptionalDeletedWorkloadUser(Set<String> deletedWorkloadUsers) {
        if (deletedWorkloadUsers == null || deletedWorkloadUsers.isEmpty()) {
            return Optional.empty();
        } else if (deletedWorkloadUsers.size() == 1) {
            return Optional.of(Iterables.getOnlyElement(deletedWorkloadUsers));
        } else {
            throw new BadRequestException("Only 1 deleted workload user is supported");
        }
    }

    @Override
    @CustomPermissionCheck
    public SyncOperationStatus setPassword(SetPasswordRequest request) {
        String userCrn = checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("setPassword() requested for user {} in account {}", userCrn, accountId);

        Operation setPasswordOperation = passwordService.setPasswordWithCustomPermissionCheck(accountId, userCrn,
                request.getPassword(), nullToEmpty(request.getEnvironments()), AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return checkOperationRejected(operationToSyncOperationStatus.convert(setPasswordOperation));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public SyncOperationStatus getSyncOperationStatus(String operationId) {
        checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("getSyncOperationStatus() requested for operation '{}' in account '{}'", operationId, accountId);
        return getSyncOperationStatusInternal(accountId, operationId);
    }

    @Override
    @InternalOnly
    public SyncOperationStatus getSyncOperationStatusInternal(@AccountId String accountId, String operationId) {
        return operationToSyncOperationStatus.convert(
                operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public SyncOperationStatus getLastSyncOperationStatus(@ResourceCrn String environmentCrn) {
        Crn envCrn = Crn.safeFromString(environmentCrn);
        EnvironmentUserSyncState userSyncState = environmentUserSyncStateCalculator.calculateEnvironmentUserSyncState(envCrn.getAccountId(), envCrn);
        return operationToSyncOperationStatus.convert(
                operationService.getOperationForAccountIdAndOperationId(envCrn.getAccountId(), userSyncState.getLastUserSyncOperationId()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public EnvironmentUserSyncState getUserSyncState(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Crn envCrn = Crn.safeFromString(environmentCrn);
        return environmentUserSyncStateCalculator.calculateEnvironmentUserSyncState(accountId, envCrn);
    }

    @Override
    @InternalOnly
    public SyncOperationStatus preSynchronize(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Operation operation = preUserSyncService.runUserSyncTasks(environmentCrn, accountId);
        SyncOperationStatus syncOperationStatus = operationToSyncOperationStatus.convert(operation);
        return checkOperationRejected(syncOperationStatus);
    }

    @Override
    @InternalOnly
    public SyncOperationStatus postSynchronize(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Operation operation = postUserSyncService.runUserSyncTasks(environmentCrn, accountId);
        SyncOperationStatus syncOperationStatus = operationToSyncOperationStatus.convert(operation);
        return checkOperationRejected(syncOperationStatus);
    }

    private String determineAccountId(String callingActor, String requestedAccountId) {
        if (requestedAccountId == null) {
            return ThreadBasedUserCrnProvider.getAccountId();
        }
        String callingActorAccountId = Crn.safeFromString(callingActor).getAccountId();
        if (!callingActorAccountId.equals(requestedAccountId) && !RegionAwareInternalCrnGeneratorUtil.isInternalCrn(callingActor)) {
            throw new ForbiddenException(String.format("Actor %s does not belong to the request account %s", callingActor, requestedAccountId));
        }
        return requestedAccountId;
    }

    private SyncOperationStatus checkOperationRejected(SyncOperationStatus syncOperationStatus) {
        if (syncOperationStatus.getStatus() == SynchronizationStatus.REJECTED) {
            throw new SyncOperationAlreadyRunningException(syncOperationStatus.getError());
        }
        return syncOperationStatus;
    }

    private String checkActorCrn() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new BadRequestException("Actor CRN must be provided");
        }
        return userCrn;
    }

    private Set<String> nullToEmpty(Set<String> set) {
        return set == null ? Set.of() : set;
    }
}
