package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.configuration.UsersyncConfig;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);

    @Inject
    private Clock clock;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @Inject
    private CrnService crnService;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_TASK_EXECUTOR)
    private AsyncTaskExecutor asyncTaskExecutor;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaPasswordValidator freeIpaPasswordValidator;

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    public Operation setPassword(String accountId, String actorCrn, String userCrn, String password, Set<String> environmentCrnFilter) {
        List<Stack> stacks = getStacksForSetPassword(accountId, userCrn, password, environmentCrnFilter);
        return setPasswordForStacks(accountId, actorCrn, userCrn, password, environmentCrnFilter, stacks);
    }

    public Operation setPasswordWithCustomPermissionCheck(String accountId, String actorCrn, String userCrn,
            String password, Set<String> environmentCrnFilter, AuthorizationResourceAction action) {
        List<Stack> stacks = getStacksForSetPassword(accountId, userCrn, password, environmentCrnFilter);
        List<String> relatedEnvironmentCrns = stacks.stream().map(stack -> stack.getEnvironmentCrn()).collect(Collectors.toList());
        commonPermissionCheckingUtils.checkPermissionForUserOnResources(action, actorCrn, relatedEnvironmentCrns);
        return setPasswordForStacks(accountId, actorCrn, userCrn, password, environmentCrnFilter, stacks);
    }

    private Operation setPasswordForStacks(String accountId, String actorCrn, String userCrn, String password,
            Set<String> environmentCrnFilter, List<Stack> stacks) {
        Operation operation = operationService.startOperation(accountId, OperationType.SET_PASSWORD,
                environmentCrnFilter, List.of(userCrn));
        if (operation.getStatus() == OperationState.RUNNING) {
            asyncSetPasswords(operation.getOperationId(), accountId, actorCrn, userCrn, password, stacks);
        }
        return operation;
    }

    private List<Stack> getStacksForSetPassword(String accountId, String userCrn, String password, Set<String> environmentCrnFilter) {
        LOGGER.debug("setting password for user {} in account {}", userCrn, accountId);
        freeIpaPasswordValidator.validate(password);
        List<Stack> stacks = stackService.getMultipleByEnvironmentCrnOrChildEnvironmantCrnAndAccountId(environmentCrnFilter, accountId);
        if (stacks.isEmpty()) {
            LOGGER.warn("No stacks found for accountId {}", accountId);
            throw new NotFoundException("No matching FreeIPA stacks found for accountId " + accountId);
        }
        LOGGER.debug("Found {} matching stacks for accountId {}", stacks.size(), accountId);
        return stacks;
    }

    private void asyncSetPasswords(String operationId, String accountId, String actorCrn, String userCrn, String password, List<Stack> stacks) {
        try {
            MDCBuilder.addOperationId(operationId);
            asyncTaskExecutor.submit(() -> internalSetPasswords(operationId, accountId, actorCrn, userCrn, password, stacks));
        } finally {
            MDCBuilder.removeOperationId();
        }
    }

    private void internalSetPasswords(String operationId, String accountId, String actorCrn, String userCrn, String password, List<Stack> stacks) {
        try {
            String userId = getUserIdFromUserCrn(actorCrn, userCrn);

            Optional<Instant> expirationInstant = calculateExpirationTime(actorCrn, accountId);

            List<SetPasswordRequest> requests = new ArrayList<>();
            for (Stack stack : stacks) {
                requests.add(triggerSetPassword(stack, stack.getEnvironmentCrn(), userId, userCrn, password, expirationInstant));
            }

            List<SuccessDetails> success = new ArrayList<>();
            List<FailureDetails> failure = new ArrayList<>();
            for (SetPasswordRequest request : requests) {
                try {
                    waitSetPassword(request);
                    success.add(new SuccessDetails(request.getEnvironment()));
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted while setting passwords for user {} in account {}", userCrn, accountId);
                    throw e;
                } catch (Exception e) {
                    LOGGER.debug("Failed to set password for user {} in environment {}", userCrn, request.getEnvironment(), e);
                    failure.add(new FailureDetails(request.getEnvironment(), e.getLocalizedMessage()));
                }
            }
            operationService.completeOperation(accountId, operationId, success, failure);
        } catch (InterruptedException e) {
            operationService.failOperation(accountId, operationId, e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            operationService.failOperation(accountId, operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    @VisibleForTesting
    Optional<Instant> calculateExpirationTime(String actorCrn, String accountId) {
        LOGGER.debug("calculating expiration time for password in account {}", accountId);
        UserManagementProto.Account account = umsClient.getAccountDetails(INTERNAL_ACTOR_CRN, accountId, MDCUtils.getRequestId());
        if (account.hasPasswordPolicy()) {
            long maxLifetime = account.getPasswordPolicy().getWorkloadPasswordMaxLifetime();
            if (maxLifetime != 0L) {
                LOGGER.debug("Calculating password expiration by adding lifetime '{}' to current time", maxLifetime);
                return Optional.of(clock.getCurrentInstant().plusMillis(maxLifetime));
            } else {
                LOGGER.debug("Password policy lifetime is {}. Using max expiration time", maxLifetime);
                return Optional.empty();
            }
        } else {
            LOGGER.debug("Account {} does not have a password policy. Using max expiration time for password", accountId);
            return Optional.empty();
        }
    }

    private String getUserIdFromUserCrn(String actorCrn, String userCrn) {
        Crn crn = Crn.safeFromString(userCrn);
        switch (crn.getResourceType()) {
            case USER:
                return umsClient.getUserDetails(actorCrn, userCrn, MDCUtils.getRequestId()).getWorkloadUsername();
            case MACHINE_USER:
                return umsClient.getMachineUserDetails(actorCrn,
                        userCrn,
                        Crn.fromString(actorCrn).getAccountId(),
                        MDCUtils.getRequestId()).getWorkloadUsername();
            default:
                throw new IllegalArgumentException(String.format("UserCrn %s is not of resource type USER or MACHINE_USER", userCrn));
        }
    }

    private SetPasswordRequest triggerSetPassword(Stack stack, String environment, String username, String userCrn,
            String password, Optional<Instant> expirationInstant) {
        SetPasswordRequest request = new SetPasswordRequest(stack.getId(), environment, username, userCrn, password, expirationInstant);
        freeIpaFlowManager.notify(request);
        return request;
    }

    private void waitSetPassword(SetPasswordRequest request) throws InterruptedException {
        SetPasswordResult result = request.await();
        if (result.getStatus().equals(EventStatus.FAILED)) {
            throw new OperationException(result.getErrorDetails());
        }
    }
}