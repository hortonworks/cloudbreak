package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.freeipa.user.SyncOperationToSyncOperationStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.SyncOperation;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
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
    private SyncOperationStatusService syncOperationStatusService;

    @Inject
    private SyncOperationToSyncOperationStatus syncOperationToSyncOperationStatus;

    @Inject
    private FreeIpaPasswordValidator freeIpaPasswordValidator;

    public SyncOperationStatus setPassword(String accountId, String userCrn, String password, Set<String> envs) {
        LOGGER.debug("setting password for user {} in account {}", userCrn, accountId);
        freeIpaPasswordValidator.validate(password);
        List<Stack> stacks;
        List<Stack> allStacks = stackService.getAllByAccountId(accountId);
        if (envs != null && !envs.isEmpty()) {
            LOGGER.debug("Environment filter provided for environments :{}", envs);
            stacks = allStacks.stream()
                    .filter(stack -> envs.contains(stack.getEnvironmentCrn()))
                    .peek(stack -> LOGGER.debug("Found matching stack for environment {}", stack.getEnvironmentCrn()))
                    .collect(Collectors.toList());
        } else {
            stacks = allStacks;
        }

        if (stacks.isEmpty()) {
            LOGGER.warn("No stacks found for accountId {}", accountId);
            throw new NotFoundException("No stacks found for accountId " + accountId);
        }

        SyncOperation syncOperation = syncOperationStatusService.startOperation(accountId, SyncOperationType.SET_PASSWORD,
                envs == null ? List.of() : List.copyOf(envs),
                List.of(userCrn));
        if (syncOperation.getStatus() == SynchronizationStatus.RUNNING) {
            asyncTaskExecutor.submit(() -> asyncSetPasswords(syncOperation.getOperationId(), accountId, userCrn, password, stacks));
        }

        return syncOperationToSyncOperationStatus.convert(syncOperation);
    }

    private void asyncSetPasswords(String operationId, String accountId, String userCrn, String password, List<Stack> stacks) {
        try {
            String userId = getUserIdFromUserCrn(userCrn);

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
            syncOperationStatusService.completeOperation(operationId, success, failure);
        } catch (InterruptedException e) {
            syncOperationStatusService.failOperation(operationId, e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            syncOperationStatusService.failOperation(operationId, e.getLocalizedMessage());
            throw e;
        }
    }

    private String getUserIdFromUserCrn(String userCrn) {
        com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User user = umsClient.getUserDetails(userCrn, userCrn, Optional.empty());
        return user.getWorkloadUsername();
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
}