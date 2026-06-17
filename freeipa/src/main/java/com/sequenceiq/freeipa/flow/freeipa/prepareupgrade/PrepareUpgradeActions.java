package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_STARTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CLUSTER_OPERATION;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupComplete;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionRequest;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class PrepareUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeActions.class);

    private static final String TEST_LB_CREATED = "TEST_LB_CREATED";

    private static final String FAILURE_EXCEPTION = "FAILURE_EXCEPTION";

    @Bean(name = "PREPARE_UPGRADE_LB_CONFIGURATION_STATE")
    public Action<?, ?> prepareUpgradeLbConfiguration() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeTriggerEvent.class) {

            @Inject
            private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Override
            protected void prepareExecution(PrepareUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
            }

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_PREPARE_UPGRADE_STARTED);
                Long stackId = payload.getResourceId();
                if (!CloudPlatform.AWS.name().equals(context.getStack().getCloudPlatform())) {
                    LOGGER.debug("Stack is not on AWS, skipping prepare upgrade LB validation");
                    sendEvent(context, new StackEvent(PREPARE_UPGRADE_FINISHED_EVENT.event(), stackId));
                } else if (freeIpaLoadBalancerService.findByStackId(stackId).isPresent()) {
                    LOGGER.debug("LoadBalancer already exists for stack, permission already validated");
                    sendEvent(context, new StackEvent(PREPARE_UPGRADE_FINISHED_EVENT.event(), stackId));
                } else {
                    variables.put(TEST_LB_CREATED, true);
                    stackUpdater().updateStackStatus(context.getStack(), CLUSTER_OPERATION, "Preparing FreeIPA upgrade: creating temporary load balancer");
                    LoadBalancer loadBalancer = freeIpaLoadBalancerConfigurationService.createLoadBalancerConfiguration(stackId, context.getStack().getName());
                    freeIpaLoadBalancerService.save(loadBalancer);
                    sendEvent(context, new StackEvent(PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT.event(), stackId));
                }
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_LB_PROVISION_STATE")
    public Action<?, ?> prepareUpgradeLbProvision() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CLUSTER_OPERATION, "Preparing FreeIPA upgrade: provisioning temporary load balancer");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new PrepareUpgradeLbProvisionRequest(context.getStack().getId(),
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_METADATA_COLLECTION_STATE")
    public Action<?, ?> prepareUpgradeMetadataCollection() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeLbProvisionSuccess.class) {

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeLbProvisionSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CLUSTER_OPERATION,
                        "Preparing FreeIPA upgrade: collecting load balancer metadata");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new PrepareUpgradeMetadataCollectionRequest(context.getStack().getId(),
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_LB_DELETION_STATE")
    public Action<?, ?> prepareUpgradeLbDeletion() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeMetadataCollectionSuccess.class) {

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeMetadataCollectionSuccess payload, Map<Object, Object> variables) {
                stackUpdater().updateStackStatus(context.getStack(), CLUSTER_OPERATION, "Preparing FreeIPA upgrade: removing temporary load balancer");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new PrepareUpgradeLbDeletionRequest(context.getStack().getId(),
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_LB_DB_CLEANUP_STATE")
    public Action<?, ?> prepareUpgradeLbDbCleanup() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeLbDeletionSuccess.class) {

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Inject
            private ResourceService resourceService;

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeLbDeletionSuccess payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Cleaning up load balancer DB records for stack {}", stackId);
                freeIpaLoadBalancerService.delete(stackId);
                resourceService.findAllByStackId(stackId).stream()
                        .filter(r -> ResourceType.getAwsLbResourceTypes().contains(r.getResourceType()))
                        .forEach(r -> resourceService.deleteByStackIdAndNameAndType(stackId, r.getResourceName(), r.getResourceType()));
                sendEvent(context, new StackEvent(PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT.event(), stackId));
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_FINISHED_STATE")
    public Action<?, ?> prepareUpgradeFinished() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                String operationId = getOperationId(variables);
                operationService.completeOperation(context.getStack().getAccountId(), operationId,
                        List.of(), List.of());
                stackUpdater().updateStackStatus(context.getStack(), AVAILABLE, "FreeIPA upgrade preparation completed");
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_PREPARE_UPGRADE_FINISHED);
                sendEvent(context, new StackEvent(PREPARE_UPGRADE_FINALIZED_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_FAILURE_CLEANUP_STATE")
    public Action<?, ?> prepareUpgradeFailureCleanup() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeFailureEvent.class) {

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeFailureEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                variables.put(FAILURE_EXCEPTION, payload.getException());
                if (Boolean.TRUE.equals(variables.getOrDefault(TEST_LB_CREATED, false))) {
                    LOGGER.debug("Attempting cloud LB resource cleanup before failure handling for stack {}", stackId);
                    sendEvent(context);
                } else {
                    LOGGER.debug("No test LB was created, skipping cloud cleanup for stack {}", stackId);
                    sendEvent(context, new PrepareUpgradeFailureCleanupComplete(stackId));
                }
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new PrepareUpgradeFailureCleanupRequest(context.getStack().getId(),
                        context.getCloudContext(), context.getCloudCredential(), context.getCloudStack());
            }

            @Override
            protected Object getFailurePayload(PrepareUpgradeFailureEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new PrepareUpgradeFailureCleanupComplete(payload.getResourceId());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_FAILED_STATE")
    public Action<?, ?> prepareUpgradeFailed() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {

            @Inject
            private OperationService operationService;

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Inject
            private ResourceService resourceService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                Exception failureException = (Exception) variables.get(FAILURE_EXCEPTION);
                String errorReason = getErrorReason(failureException);
                LOGGER.error("Prepare upgrade failed for stack {}: {}", stackId, errorReason, failureException);

                if (Boolean.TRUE.equals(variables.getOrDefault(TEST_LB_CREATED, false))) {
                    try {
                        freeIpaLoadBalancerService.delete(stackId);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to clean up load balancer DB record during failure handling", e);
                    }
                    try {
                        resourceService.findAllByStackId(stackId).stream()
                                .filter(r -> ResourceType.getAwsLbResourceTypes().contains(r.getResourceType()))
                                .forEach(r -> resourceService.deleteByStackIdAndNameAndType(stackId, r.getResourceName(), r.getResourceType()));
                    } catch (Exception e) {
                        LOGGER.warn("Failed to clean up resource DB records during failure handling", e);
                    }
                }

                String operationId = getOperationId(variables);
                FailureDetails failureDetails = new FailureDetails(context.getStack().getEnvironmentCrn(), errorReason);
                operationService.failOperation(context.getStack().getAccountId(), operationId,
                        errorReason, List.of(), List.of(failureDetails));
                stackUpdater().updateStackStatus(context.getStack(), AVAILABLE, "FreeIPA upgrade preparation failed: " + errorReason);
                getEventService().sendEventAndNotification(context.getStack(), context.getFlowTriggerUserCrn(),
                        FREEIPA_PREPARE_UPGRADE_FAILED, List.of(errorReason));
                sendEvent(context, new StackEvent(PREPARE_UPGRADE_FAILURE_HANDLED_EVENT.event(), stackId));
            }
        };
    }
}
