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
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class MultiAzMigrationInitActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationInitActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Bean(name = "MULTI_AZ_MIGRATION_INIT_STATE")
    public Action<?, ?> multiAzMigrationInitAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationInitTriggerEvent.class) {

            @Override
            protected void prepareExecution(MultiAzMigrationInitTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
                variables.put(HAS_LOAD_BALANCER, freeIpaLoadBalancerService.findByStackId(payload.getResourceId()).isPresent());
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

    @Bean(name = "MULTI_AZ_MIGRATION_LB_UPDATE_STATE")
    public Action<?, ?> multiAzMigrationLbUpdateAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationInitResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationInitResult payload, Map<Object, Object> variables) {
                if (isAws(context) && hasLoadBalancer(variables)) {
                    LOGGER.info("Starting load balancer subnet update for stack: {}", context.getStack().getName());
                    stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS,
                            "Updating FreeIPA load balancer subnets.");
                    sendEvent(context, new MultiAzMigrationLbUpdateHandlerRequest(payload.getResourceId(), payload.getOperationId(),
                            context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
                } else {
                    LOGGER.debug("Skipping load balancer subnet update for stack: {}", context.getStack().getName());
                    sendEvent(context, new MultiAzMigrationLbUpdateResult(payload.getResourceId(), payload.getOperationId()));
                }
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_LB_WAIT_STATE")
    public Action<?, ?> multiAzMigrationLbWaitAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationLbUpdateResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationLbUpdateResult payload, Map<Object, Object> variables) {
                if (isAws(context) && hasLoadBalancer(variables)) {
                    LOGGER.info("Waiting for load balancer to be ready for stack: {}", context.getStack().getName());
                    stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS,
                            "Waiting for FreeIPA load balancer to be ready.");
                    sendEvent(context, new MultiAzMigrationLbWaitHandlerRequest(payload.getResourceId(), payload.getOperationId(),
                            context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
                } else {
                    LOGGER.debug("Skipping load balancer wait for stack: {}", context.getStack().getName());
                    sendEvent(context, new MultiAzMigrationLbWaitResult(payload.getResourceId(), payload.getOperationId()));
                }
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_LB_METADATA_COLLECTION_STATE")
    public Action<?, ?> multiAzMigrationLbMetadataCollectionAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationLbWaitResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationLbWaitResult payload, Map<Object, Object> variables) {
                if (isAws(context) && hasLoadBalancer(variables)) {
                    LOGGER.info("Refreshing load balancer metadata for stack: {}", context.getStack().getName());
                    stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS,
                            "Refreshing FreeIPA load balancer metadata.");
                    sendEvent(context, new MultiAzMigrationLbMetadataCollectionHandlerRequest(payload.getResourceId(), payload.getOperationId(),
                            context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
                } else {
                    LOGGER.debug("Skipping load balancer metadata collection for stack: {}", context.getStack().getName());
                    sendEvent(context, new MultiAzMigrationLbMetadataCollectionResult(payload.getResourceId(), payload.getOperationId()));
                }
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_LB_DNS_UPDATE_STATE")
    public Action<?, ?> multiAzMigrationLbDnsUpdateAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationLbMetadataCollectionResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationLbMetadataCollectionResult payload, Map<Object, Object> variables) {
                if (isAws(context) && hasLoadBalancer(variables)) {
                    LOGGER.info("Starting load balancer DNS A record update for stack: {}", context.getStack().getName());
                    stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS,
                            "Updating FreeIPA load balancer DNS A record.");
                    sendEvent(context, new MultiAzMigrationLbDnsUpdateHandlerRequest(payload.getResourceId(), payload.getOperationId()));
                } else {
                    LOGGER.debug("Skipping load balancer DNS A record update for stack: {}", context.getStack().getName());
                    sendEvent(context, new MultiAzMigrationLbDnsUpdateResult(payload.getResourceId(), payload.getOperationId()));
                }
            }
        };
    }

    @Bean(name = "MULTI_AZ_MIGRATION_INIT_FINISHED_STATE")
    public Action<?, ?> multiAzMigrationInitFinishedAction() {
        return new AbstractMultiAzMigrationInitAction<>(MultiAzMigrationLbDnsUpdateResult.class) {

            @Override
            protected void doExecute(StackContext context, MultiAzMigrationLbDnsUpdateResult payload, Map<Object, Object> variables) {
                LOGGER.debug("Multi-AZ migration initialization completed for stack: {}", context.getStack().getName());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS,
                        "FreeIPA multi-AZ migration initialization completed.");
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
