package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.user.OperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @Inject
    private CrnService crnService;

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private AsyncTaskExecutor asyncTaskExecutor;

    @Inject
    private OperationStatusService operationStatusService;

    @Inject
    private OperationToSyncOperationStatus operationToSyncOperationStatus;

    @Inject
    private FreeIpaPasswordValidator freeIpaPasswordValidator;

    public SyncOperationStatus setPassword(String accountId, String actorCrn, String userCrn, String password, Set<String> environmentCrnFilter) {
        LOGGER.debug("setting password for user {} in account {}", userCrn, accountId);
        freeIpaPasswordValidator.validate(password);

        List<Stack> stacks = getStacks(accountId, environmentCrnFilter);
        if (stacks.isEmpty()) {
            LOGGER.warn("No stacks found for accountId {}", accountId);
            throw new NotFoundException("No matching FreeIPA stacks found for accountId " + accountId);
        }
        LOGGER.debug("Found {} matching stacks for accountId {}", stacks.size(), accountId);

        Operation operation = operationStatusService.startOperation(accountId, OperationType.SET_PASSWORD,
                environmentCrnFilter, List.of(userCrn));
        if (operation.getStatus() == OperationState.RUNNING) {
            MDCBuilder.addFlowId(operation.getOperationId());
            asyncTaskExecutor.submit(() -> asyncSetPasswords(operation.getOperationId(), accountId, actorCrn, userCrn, password, stacks));
        }

        return operationToSyncOperationStatus.convert(operation);
    }

    private void asyncSetPasswords(String operationId, String accountId, String actorCrn, String userCrn, String password, List<Stack> stacks) {
        try {
            String userId = getUserIdFromUserCrn(actorCrn, userCrn);

            List<SetPasswordRequest> requests = new ArrayList<>();
            for (Stack stack : stacks) {
                requests.add(triggerSetPassword(stack, stack.getEnvironmentCrn(), userId, password));
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
            operationStatusService.completeOperation(operationId, success, failure);
        } catch (InterruptedException e) {
            operationStatusService.failOperation(operationId, e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            operationStatusService.failOperation(operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    private String getUserIdFromUserCrn(String actorCrn, String userCrn) {
        Crn crn = Crn.safeFromString(userCrn);
        switch (crn.getResourceType()) {
            case USER:
                return umsClient.getUserDetails(actorCrn, userCrn, Optional.empty()).getWorkloadUsername();
            case MACHINE_USER:
                return umsClient.getMachineUserDetails(actorCrn, userCrn, Optional.empty()).getWorkloadUsername();
            default:
                throw new IllegalArgumentException(String.format("UserCrn %s is not of resource type USER or MACHINE_USER", userCrn));
        }
    }

    private SetPasswordRequest triggerSetPassword(Stack stack, String environment, String username, String password) {
        SetPasswordRequest request = new SetPasswordRequest(stack.getId(), environment, username, password);
        freeIpaFlowManager.notify(request);
        return request;
    }

    private void waitSetPassword(SetPasswordRequest request) throws InterruptedException {
        SetPasswordResult result = request.await();
        if (result.getStatus().equals(EventStatus.FAILED)) {
            throw new OperationException(result.getErrorDetails());
        }
    }

    private List<Stack> getStacks(String accountId, Set<String> environmentCrnFilter) {
        if (environmentCrnFilter.isEmpty()) {
            LOGGER.debug("Retrieving all stacks for account {}", accountId);
            return stackService.getAllByAccountId(accountId);
        } else {
            LOGGER.debug("Retrieving stacks for account {} that match environment crns {}", accountId, environmentCrnFilter);
            return stackService.getMultipleByEnvironmentCrnAndAccountId(environmentCrnFilter, accountId);
        }
    }
}