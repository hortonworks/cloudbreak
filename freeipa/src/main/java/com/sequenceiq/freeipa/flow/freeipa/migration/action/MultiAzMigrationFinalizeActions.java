package com.sequenceiq.freeipa.flow.freeipa.migration.action;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class MultiAzMigrationFinalizeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationFinalizeActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Bean(name = "MULTI_AZ_MIGRATION_FINALIZE_STATE")
    public Action<?, ?> multiAzMigrationFinalizeAction() {
        return new AbstractMultiAzMigrationFinalizeAction<>(MultiAzMigrationFinalizeTriggerEvent.class) {

            @Override
            protected void prepareExecution(MultiAzMigrationFinalizeTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
            }

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationFinalizeTriggerEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.info("Finalizing multi-AZ migration for stack: {}", stack.getName());
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPDATE_COMPLETE, "FreeIPA multi-AZ migration completed successfully.");
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                operationService.completeOperation(stack.getAccountId(), payload.getOperationId(), Set.of(successDetails), Set.of());
                getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FINISHED);
                enableStatusChecker(stack, "Multi-AZ migration completed successfully.");
                sendEvent(context, MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE")
    public Action<?, ?> multiAzMigrationFinalizeFailedAction() {
        return new AbstractMultiAzMigrationFinalizeAction<>(MultiAzMigrationFinalizeFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationFinalizeFailedEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.error("Multi-AZ migration finalization failed with: ", payload.getException());
                String errorReason = getErrorReason(payload.getException());
                operationService.failOperation(stack.getAccountId(), getOperationId(variables),
                        "FreeIPA multi-AZ migration finalization failed: " + errorReason);
                sendEvent(context, MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
