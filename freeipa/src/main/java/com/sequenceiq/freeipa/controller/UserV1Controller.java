package com.sequenceiq.freeipa.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
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

    @Override
    @CustomPermissionCheck
    public SyncOperationStatus synchronizeUser(SynchronizeUserRequest request) {
        String userCrn = checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("synchronizeUser() is deprecated, sync is possible only for environments.");
        Set<String> environmentCrnFilter = request == null ? Set.of() : nullToEmpty(request.getEnvironments());
        Operation syncOperation = userSyncService.synchronizeUsersWithCustomPermissionCheck(accountId, userCrn,
                environmentCrnFilter, WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED, AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return checkOperationRejected(operationToSyncOperationStatus.convert(syncOperation));
    }

    @Override
    @AccountIdNotNeeded
    @CustomPermissionCheck
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkActorCrn();
        String accountId = determineAccountId(userCrn, request.getAccountId());
        LOGGER.debug("synchronizeAllUsers() requested for account {}", accountId);
        Operation syncOperation = userSyncService.synchronizeUsersWithCustomPermissionCheck(accountId, userCrn,
                nullToEmpty(request.getEnvironments()), request.getWorkloadCredentialsUpdateType(),
                AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
        return checkOperationRejected(operationToSyncOperationStatus.convert(syncOperation));
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
    public SyncOperationStatus getSyncOperationStatus(@NotNull String operationId) {
        checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("getSyncOperationStatus() requested for operation '{}' in account '{}'", operationId, accountId);
        return getSyncOperationStatusInternal(accountId, operationId);
    }

    @Override
    @InternalOnly
    public SyncOperationStatus getSyncOperationStatusInternal(@AccountId String accountId, @NotNull String operationId) {
        return operationToSyncOperationStatus.convert(
                operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public SyncOperationStatus getLastSyncOperationStatus(@ResourceCrn @TenantAwareParam @NotEmpty String environmentCrn) {
        Crn envCrn = Crn.safeFromString(environmentCrn);
        EnvironmentUserSyncState userSyncState = environmentUserSyncStateCalculator.calculateEnvironmentUserSyncState(envCrn.getAccountId(), envCrn);
        return operationToSyncOperationStatus.convert(
                operationService.getOperationForAccountIdAndOperationId(envCrn.getAccountId(), userSyncState.getLastUserSyncOperationId()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public EnvironmentUserSyncState getUserSyncState(@ResourceCrn @TenantAwareParam @NotEmpty String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Crn envCrn = Crn.safeFromString(environmentCrn);

        return environmentUserSyncStateCalculator.calculateEnvironmentUserSyncState(accountId, envCrn);
    }

    private String determineAccountId(String callingActor, String requestedAccountId) {
        if (requestedAccountId == null) {
            return ThreadBasedUserCrnProvider.getAccountId();
        }
        String callingActorAccountId = Crn.safeFromString(callingActor).getAccountId();
        if (!callingActorAccountId.equals(requestedAccountId) && !InternalCrnBuilder.isInternalCrn(callingActor)) {
            throw new AccessDeniedException(String.format("Actor %s does not belong to the request account %s", callingActor, requestedAccountId));
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
