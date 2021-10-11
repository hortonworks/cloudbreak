package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterManagerUpgradeSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedCmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeSuccess;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageComponentUpdaterService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class ClusterUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeActions.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterUpgradeService clusterUpgradeService;

    @Bean(name = "CLUSTER_UPGRADE_INIT_STATE")
    public Action<?, ?> initClusterUpgrade() {
        return new AbstractClusterUpgradeAction<>(ClusterUpgradeTriggerEvent.class) {

            @Inject
            private ImageComponentUpdaterService imageComponentUpdaterService;

            @Inject
            private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                try {
                    UpgradeImageInfo images = imageComponentUpdaterService.updateForUpgrade(payload.getImageId(), payload.getResourceId());
                    variables.put(CURRENT_IMAGE, images.getCurrentStatedImage());
                    variables.put(TARGET_IMAGE, images.getTargetStatedImage());
                    clusterUpgradeTargetImageService.saveImage(context.getStackId(), images.getTargetStatedImage());
                    clusterUpgradeService.initUpgradeCluster(context.getStackId(), getTargetImage(variables));
                    Selectable event = new ClusterUpgradeInitRequest(context.getStackId(), isPatchUpgrade(images.getCurrentStatedImage().getImage(),
                            images.getTargetStatedImage().getImage()));
                    sendEvent(context, event.selector(), event);
                } catch (Exception e) {
                    LOGGER.error("Error during updating cluster components with image id: [{}]", payload.getImageId(), e);
                    ClusterUpgradeFailedEvent upgradeFailedEvent =
                            new ClusterUpgradeFailedEvent(payload.getResourceId(), e, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
                    sendEvent(context, upgradeFailedEvent);
                }
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeTriggerEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return ClusterUpgradeFailedEvent.from(payload, ex, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
            }

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeTriggerEvent payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
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
                Image currentImage = getCurrentImage(variables).getImage();
                Image targetImage = getTargetImage(variables).getImage();
                clusterUpgradeService.upgradeClusterManager(context.getStackId());
                Selectable event = new ClusterManagerUpgradeRequest(context.getStackId(),
                        !clusterUpgradeService.isClusterRuntimeUpgradeNeeded(currentImage, targetImage));
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
                Image currentImage = getCurrentImage(variables).getImage();
                Image targetImage = getTargetImage(variables).getImage();
                boolean clusterRuntimeUpgradeNeeded = clusterUpgradeService.upgradeCluster(context.getStackId(), currentImage, targetImage);
                Selectable event;
                if (clusterRuntimeUpgradeNeeded) {
                    event = new ClusterUpgradeRequest(context.getStackId(), isPatchUpgrade(currentImage, targetImage));
                } else {
                    event = new ClusterUpgradeSuccess(context.getStackId());
                }
                sendEvent(context, event.selector(), event);
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

            @Inject
            private StackImageService stackImageService;

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    ClusterUpgradeSuccess payload) {
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeSuccess payload, Map<Object, Object> variables) {
                StatedImage currentImage = getCurrentImage(variables);
                StatedImage targetImage = getTargetImage(variables);
                clusterUpgradeService.clusterUpgradeFinished(context.getStackId(), currentImage, targetImage);
                stackImageService.removeImageByComponentName(context.getStackId(), TARGET_IMAGE);
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

            @Value("${cb.upgrade.failure.sync.sdx.enabled}")
            private boolean syncAfterFailureEnabled;

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
                if (syncAfterFailureEnabled) {
                    LOGGER.debug("Starting syncing parcel and CM version from CM to DB.");
                    try {
                        Set<Image> candidateImages = new HashSet<>();
                        Optional.ofNullable(getCurrentImage(variables)).ifPresent(si -> candidateImages.add(si.getImage()));
                        Optional.ofNullable(getTargetImage(variables)).ifPresent(si -> candidateImages.add(si.getImage()));
                        ClusterUpgradeFailedCmSyncRequest cmSyncRequest =
                                new ClusterUpgradeFailedCmSyncRequest(payload.getResourceId(), payload.getException(), payload.getDetailedStatus(),
                                        candidateImages);
                        sendEvent(context, cmSyncRequest);
                    } catch (Exception e) {
                        LOGGER.warn("Error starting syncing CM version to DB, syncing skipped: ", e);
                        sendEvent(context, new ClusterUpgradeFailHandledRequest(payload.getResourceId(), payload.getException(), payload.getDetailedStatus()));
                    }
                } else {
                    LOGGER.debug("Syncing from CM to DB is not enabled.");
                    sendEvent(context, new ClusterUpgradeFailHandledRequest(payload.getResourceId(), payload.getException(), payload.getDetailedStatus()));
                }
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeFailedEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    private boolean isPatchUpgrade(Image currentImage, Image targetImage) {
        StackDetails currentImageStackDetails = currentImage.getStackDetails();
        StackDetails targetImageStackDetails = targetImage.getStackDetails();
        return currentImageStackDetails != null && targetImageStackDetails != null
                && currentImageStackDetails.getVersion().equals(targetImageStackDetails.getVersion());
    }
}
