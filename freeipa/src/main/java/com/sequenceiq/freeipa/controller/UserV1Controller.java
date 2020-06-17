package com.sequenceiq.freeipa.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;
import com.sequenceiq.freeipa.converter.freeipa.user.OperationToSyncOperationStatus;
import com.sequenceiq.freeipa.service.freeipa.user.EnvironmentUserSyncStateCalculator;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Controller
@InternalReady
@AuthorizationResource
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
    // TODO we need to handle the case when environments is empty and the sync will applied to all env in account
    // until that resource based authz cannot be applied
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
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
                throw new BadRequestException(String.format("UserCrn %s is not of resource type USER or MACHINE_USER", userCrn));
        }
        return checkOperationRejected(
                operationToSyncOperationStatus.convert(
                        userSyncService.synchronizeUsers(accountId, environmentCrnFilter, userCrnFilter, machineUserCrnFilter)));
    }

    @Override
    // TODO we need to handle the case when environments is empty and the sync will applied to all env in account
    // until that resource based authz cannot be applied
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        String userCrn = checkActorCrn();
        String accountId = determineAccountId(userCrn, request.getAccountId());

        LOGGER.debug("synchronizeAllUsers() requested for account {}", accountId);

        return checkOperationRejected(
                operationToSyncOperationStatus.convert(
                        userSyncService.synchronizeUsers(accountId, nullToEmpty(request.getEnvironments()),
                                nullToEmpty(request.getUsers()), nullToEmpty(request.getMachineUsers()))));
    }

    @Override
    // TODO we need to handle the case when environments is empty and the setPassword will applied to all env in account
    // until that resource based authz cannot be applied
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public SyncOperationStatus setPassword(SetPasswordRequest request) {
        String userCrn = checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("setPassword() requested for user {} in account {}", userCrn, accountId);

        return checkOperationRejected(
                operationToSyncOperationStatus.convert(
                        passwordService.setPassword(accountId, userCrn, userCrn, request.getPassword(), nullToEmpty(request.getEnvironments()))));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.GET_OPERATION_STATUS)
    public SyncOperationStatus getSyncOperationStatus(@NotNull String operationId) {
        checkActorCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("getSyncOperationStatus() requested for operation '{}' in account '{}'", operationId, accountId);
        return operationToSyncOperationStatus.convert(
                operationService.getOperationForAccountIdAndOperationId(accountId, operationId));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public EnvironmentUserSyncState getUserSyncState(@ResourceCrn @TenantAwareParam @NotEmpty String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Crn envCrn = Crn.safeFromString(environmentCrn);

        return environmentUserSyncStateCalculator.calculateEnvironmentUserSyncState(accountId, envCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public SyncOperationStatus synchronizeEnvironment(@ResourceCrn @TenantAwareParam @NotEmpty String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        return checkOperationRejected(
                operationToSyncOperationStatus.convert(
                        userSyncService.synchronizeUsers(accountId, Set.of(environmentCrn), Set.of(), Set.of())));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.ENVIRONMENT_READ)
//    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ENVIRONMENT_READ)
    public SyncOperationStatus synchronizeActor(@ResourceCrn @TenantAwareParam @NotEmpty String actorCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        Set<String> userCrnFilter = Sets.newHashSet();
        Set<String> machineUserCrnFilter = Sets.newHashSet();
        Crn crn = Crn.safeFromString(actorCrn);
        switch (crn.getResourceType()) {
            case USER:
                userCrnFilter.add(actorCrn);
                break;
            case MACHINE_USER:
                machineUserCrnFilter.add(actorCrn);
                break;
            default:
                throw new BadRequestException(String.format("actorCrn %s is not of resource type USER or MACHINE_USER", actorCrn));
        }
        return checkOperationRejected(
                operationToSyncOperationStatus.convert(
                        userSyncService.synchronizeUsers(accountId, Set.of(), userCrnFilter, machineUserCrnFilter)));
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
