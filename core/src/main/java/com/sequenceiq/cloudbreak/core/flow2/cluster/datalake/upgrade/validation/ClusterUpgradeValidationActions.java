package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class ClusterUpgradeValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeValidationActions.class);

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_INIT_STATE")
    public Action<?, ?> initClusterUpgradeValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationEvent.class) {

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting cluster upgrade validation flow. Target image: {}", payload.getImageId());
                cloudbreakEventService.fireCloudbreakEvent(payload.getResourceId(), UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED);
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeValidationEvent payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeDiskSpaceValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationEvent.class) {

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting disk space validation.");
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(VALIDATE_CLUSTER_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeValidationEvent payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE")
    public Action<?, ?> clusterUpgradeValidationFinished() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationFinishedEvent.class) {

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationFinishedEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                Exception exception = payload.getException();
                if (exception == null) {
                    LOGGER.info("Cluster upgrade validation finished successfully");
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FINISHED);
                } else {
                    LOGGER.info("Cluster upgrade validation finished due to and error", exception);
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_SKIPPED,
                            Collections.singletonList(exception.getMessage()));
                }
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(FINALIZE_CLUSTER_UPGRADE_VALIDATION_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event);
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeValidationFinishedEvent payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationFinishedEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_FAILED_STATE")
    public Action<?, ?> clusterUpgradeValidationFailed() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationFailureEvent.class) {

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationFailureEvent payload, Map<Object, Object> variables) {
                String errorMessage = payload.getException().getMessage();
                Long resourceId = payload.getResourceId();
                LOGGER.error("Cluster upgrade validation failed with validation error: {}", errorMessage);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FAILED,
                        Collections.singletonList(errorMessage));
                sendEvent(context, HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event(), payload);
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeValidationFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                Stack stack = stackService.getById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationFailureEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
