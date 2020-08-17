package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAIL_HANDLED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class ClusterUpgradeActions {

    private static final String CURRENT_IMAGE = "CURRENT_IMAGE";

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    @Inject
    private StackService stackService;

    @Inject
    private ClusterUpgradeService clusterUpgradeService;

    @Bean(name = "CLUSTER_UPGRADE_INIT_STATE")
    public Action<?, ?> initClusterUpgrade() {
        return new AbstractClusterUpgradeAction<>(DatalakeClusterUpgradeTriggerEvent.class) {

            @Override
            protected void prepareExecution(DatalakeClusterUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(CURRENT_IMAGE, payload.getCurrentImage());
                variables.put(TARGET_IMAGE, payload.getTargetImage());
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    DatalakeClusterUpgradeTriggerEvent payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, DatalakeClusterUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                clusterUpgradeService.initUpgradeCluster(context.getStackId(), payload.getTargetImage());
                Selectable event = new ClusterUpgradeInitRequest(context.getStackId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(DatalakeClusterUpgradeTriggerEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return ClusterUpgradeFailedEvent.from(payload, ex, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_UPGRADE_STATE")
    public Action<?, ?> upgradeClusterManager() {
        return new AbstractClusterUpgradeAction<>(ClusterUpgradeInitSuccess.class) {
            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeInitSuccess payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeInitSuccess payload, Map<Object, Object> variables) {
                StatedImage currentImage = (StatedImage) variables.get(CURRENT_IMAGE);
                StatedImage targetImage = (StatedImage) variables.get(TARGET_IMAGE);
                boolean clusterManagerUpdateNeeded = clusterUpgradeService.upgradeClusterManager(context.getStackId(), currentImage, targetImage);
                Selectable event;
                if (clusterManagerUpdateNeeded) {
                    event = new ClusterManagerUpgradeRequest(context.getStackId());
                } else {
                    event = new ClusterManagerUpgradeSuccess(context.getStackId());
                }
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeInitSuccess payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return ClusterUpgradeFailedEvent.from(payload, ex, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_STATE")
    public Action<?, ?> upgradeCluster() {
        return new AbstractClusterUpgradeAction<>(ClusterManagerUpgradeSuccess.class) {

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterManagerUpgradeSuccess payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterManagerUpgradeSuccess payload, Map<Object, Object> variables) {
                Image currentImage = getImage(variables, CURRENT_IMAGE);
                Image targetImage = getImage(variables, TARGET_IMAGE);
                boolean clusterRuntimeUpgradeNeeded = clusterUpgradeService.upgradeCluster(context.getStackId(), currentImage, targetImage);
                Selectable event;
                if (clusterRuntimeUpgradeNeeded) {
                    event = new ClusterUpgradeRequest(context.getStackId(), isPatchUpgrade(currentImage, targetImage));
                } else {
                    event = new ClusterUpgradeSuccess(context.getStackId());
                }
                sendEvent(context, event.selector(), event);
            }

            private boolean isPatchUpgrade(Image currentImage, Image targetImage) {
                StackDetails currentImageStackDetails = currentImage.getStackDetails();
                StackDetails targetImageStackDetails = targetImage.getStackDetails();
                return currentImageStackDetails != null && targetImageStackDetails != null
                        && currentImageStackDetails.getVersion().equals(targetImageStackDetails.getVersion());
            }

            private Image getImage(Map<Object, Object> variables, String key) {
                return ((StatedImage) variables.get(key)).getImage();
            }

            @Override
            protected Object getFailurePayload(ClusterManagerUpgradeSuccess payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return ClusterUpgradeFailedEvent.from(payload, ex, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_FINISHED_STATE")
    public Action<?, ?> clusterUpgradeFinished() {
        return new AbstractClusterUpgradeAction<>(ClusterUpgradeSuccess.class) {

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeSuccess payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeSuccess payload, Map<Object, Object> variables) {
                StatedImage currentImage = (StatedImage) variables.get(CURRENT_IMAGE);
                StatedImage targetImage = (StatedImage) variables.get(TARGET_IMAGE);
                clusterUpgradeService.clusterUpgradeFinished(context.getStackId(), currentImage, targetImage);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpgradeContext context) {
                return new StackEvent(ClusterUpgradeEvent.CLUSTER_UPGRADE_FINALIZED_EVENT.event(), context.getStackId());
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeSuccess payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_FAILED_STATE")
    public Action<?, ?> clusterUpgradeFailedAction() {
        return new AbstractClusterUpgradeAction<>(ClusterUpgradeFailedEvent.class) {

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                Stack stack = stackService.getById(payload.getResourceId());
                MDCBuilder.buildMdcContext(stack);
                flow.setFlowFailed(payload.getException());
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeFailedEvent payload, Map<Object, Object> variables) {
                clusterUpgradeService.handleUpgradeClusterFailure(context.getStackId(), payload.getException().getMessage(), payload.getDetailedStatus());
                sendEvent(context, CLUSTER_UPGRADE_FAIL_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeFailedEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

}
