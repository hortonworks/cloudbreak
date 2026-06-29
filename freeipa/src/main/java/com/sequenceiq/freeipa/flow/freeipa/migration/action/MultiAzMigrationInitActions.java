package com.sequenceiq.freeipa.flow.freeipa.migration.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class MultiAzMigrationInitActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationInitActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Bean(name = "MULTI_AZ_MIGRATION_INIT_STATE")
    public Action<?, ?> multiAzMigrationInitAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationInitTriggerEvent.class) {

            @Override
            protected void prepareExecution(MultiAzMigrationInitTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
            }

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationInitTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting multi-AZ migration initialization for stack: {}", context.getStack().getName());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS, "Starting FreeIPA multi-AZ migration.");
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_STARTED);
                sendEvent(context, new MultiAzMigrationInitHandlerRequest(payload.getResourceId(), payload.getOperationId()));
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_INIT_FINISHED_STATE")
    public Action<?, ?> multiAzMigrationInitFinishedAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationInitResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationInitResult payload, Map<Object, Object> variables) {
                LOGGER.debug("Multi-AZ migration DB update completed for stack: {}", context.getStack().getName());
                sendEvent(context, MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FINISHED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_INIT_FAILED_STATE")
    public Action<?, ?> multiAzMigrationInitFailedAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationInitFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationInitFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Multi-AZ migration initialization failed with: ", payload.getException());
                String errorReason = getErrorReason(payload.getException());
                operationService.failOperation(context.getStack().getAccountId(), getOperationId(variables),
                        "FreeIPA multi-AZ migration initialization failed: " + errorReason);
                sendEvent(context, MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
