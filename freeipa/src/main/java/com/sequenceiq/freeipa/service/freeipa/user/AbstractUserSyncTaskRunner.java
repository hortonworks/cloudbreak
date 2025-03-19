package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractUserSyncTaskRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserSyncTaskRunner.class);

    @Value("#{${freeipa.operation.cleanup.timeout-millis} * 0.1 }")
    private Long operationTimeout;

    @Inject
    private StackService stackService;

    @Inject
    private OperationService operationService;

    public Operation runUserSyncTasks(String environmentCrn, String accountId) {
        Operation operation = operationService.startOperation(accountId, OperationType.USER_SYNC, List.of(environmentCrn), List.of());
        MDCBuilder.addOperationId(operation.getOperationId());
        if (operation.getStatus() == OperationState.RUNNING) {
            operationService.tryWithOperationCleanup(operation.getOperationId(), accountId, () -> {
                Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
                MDCBuilder.buildMdcContext(stack);
                LOGGER.info("Starting usersync task");
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> asyncRunTask(operation.getOperationId(), accountId, stack));
            });
        } else {
            LOGGER.warn("Operation is not RUNNING, usrsync task won't start. {}", operation);
        }
        return operation;
    }

    protected abstract void asyncRunTask(String operationId, String accountId, Stack stack);

    protected Long getOperationTimeout() {
        return operationTimeout;
    }
}
