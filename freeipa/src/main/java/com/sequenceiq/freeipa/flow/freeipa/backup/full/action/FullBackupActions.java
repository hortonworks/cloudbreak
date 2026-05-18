package com.sequenceiq.freeipa.flow.freeipa.backup.full.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_FULL_BACKUP_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FINISHED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.BackupContext;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.CreateFullBackupEvent;
import com.sequenceiq.freeipa.flow.freeipa.backup.full.event.TriggerFullBackupEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Configuration
public class FullBackupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullBackupActions.class);

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "BACKUP_STATE")
    public Action<?, ?> backupAction() {
        return new AbstractBackupAction<>(TriggerFullBackupEvent.class) {

            @Override
            protected void doExecute(BackupContext context, TriggerFullBackupEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Full backup flow started with: {}", payload);
                eventSenderService.sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_FULL_BACKUP_STARTED);
                setChainedAction(variables, payload.isChained());
                setFinalChain(variables, payload.isFinalChain());
                setOperationId(variables, payload.getOperationId());
                sendEvent(context, new CreateFullBackupEvent(payload.getResourceId()));
            }
        };
    }

    @Bean(name = "BACKUP_FINISHED_STATE")
    public Action<?, ?> backupFinishedAction() {
        return new AbstractBackupAction<>(StackEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(BackupContext context, StackEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Full backup flow finished");
                eventSenderService.sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_FULL_BACKUP_FINISHED);
                if (isOperationIdSet(variables) && (!isChainedAction(variables) || isFinalChain(variables))) {
                    LOGGER.debug("Complete operation with id: [{}]", getOperationId(variables));
                    Stack stack = context.getStack();
                    SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                    operationService.completeOperation(stack.getAccountId(), getOperationId(variables), Set.of(successDetails), Set.of());
                }
                sendEvent(context, new StackEvent(FULL_BACKUP_FINISHED_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    @Bean(name = "BACKUP_FAILED_STATE")
    public Action<?, ?> backupFailedAction() {
        return new AbstractBackupAction<>(StackFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(BackupContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Full backup failed", payload.getException());
                String errorReason = getErrorReason(payload.getException());
                eventSenderService.sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(), FREEIPA_FULL_BACKUP_FAILED,
                        List.of(errorReason));
                if (isOperationIdSet(variables)) {
                    LOGGER.debug("Fail operation with id: [{}]", getOperationId(variables));
                    operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables), errorReason);
                }
                sendEvent(context, new StackEvent(FULL_BACKUP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    private String getErrorReason(Exception exception) {
        return exception == null || exception.getMessage() == null ? "Unknown error" : exception.getMessage();
    }
}
