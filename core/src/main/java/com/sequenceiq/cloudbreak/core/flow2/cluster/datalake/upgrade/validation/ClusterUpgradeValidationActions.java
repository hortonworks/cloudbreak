package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT;

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

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinalizeEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfoFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ClusterUpgradeValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeValidationActions.class);

    private static final String LOCK_COMPONENTS = "lockComponents";

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_INIT_STATE")
    public Action<?, ?> initClusterUpgradeValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting cluster upgrade validation flow. Target image: {}", payload.getImageId());
                cloudbreakEventService.fireCloudbreakEvent(payload.getResourceId(), UPDATE_IN_PROGRESS.name(),
                        ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED);
                variables.put(LOCK_COMPONENTS, payload.isLockComponents());
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationTriggerEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeImageValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationEvent payload, Map<Object, Object> variables)
                    throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
                LOGGER.info("Starting cluster upgrade image validation.");
                UpgradeImageInfo upgradeImageInfo = upgradeImageInfoFactory.create(payload.getImageId(), payload.getResourceId());
                Image targetImage = stackImageUpdateService
                        .getImageModelFromStatedImage(context.getStack(), upgradeImageInfo.getCurrentImage(), upgradeImageInfo.getTargetStatedImage());
                CloudStack cloudStack = context.getCloudStack().replaceImage(targetImage);
                ClusterUpgradeImageValidationEvent event = new ClusterUpgradeImageValidationEvent(payload.getResourceId(), payload.getImageId(), cloudStack,
                        context.getCloudCredential(), context.getCloudContext());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFailureEvent(payload.getResourceId(), ex);
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeDiskSpaceValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting disk space validation.");
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(VALIDATE_DISK_SPACE_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeDiskSpaceValidationFinishedEvent(payload.getResourceId());
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeServiceValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeDiskSpaceValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeDiskSpaceValidationFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting to validate services.");
                boolean lockComponents = (Boolean) variables.get(LOCK_COMPONENTS);
                ClusterUpgradeServiceValidationEvent event = new ClusterUpgradeServiceValidationEvent(payload.getResourceId(), lockComponents);
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeDiskSpaceValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_FINISHED_STATE")
    public Action<?, ?> clusterUpgradeValidationFinished() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationFinishedEvent payload, Map<Object, Object> variables) {
                Long resourceId = payload.getResourceId();
                Exception exception = payload.getException();
                if (exception == null) {
                    LOGGER.info("Cluster upgrade validation finished successfully");
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FINISHED);
                } else {
                    LOGGER.info("Cluster upgrade validation finished due to an error", exception);
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_SKIPPED,
                            Collections.singletonList(exception.getMessage()));
                }
                ClusterUpgradeValidationFinalizeEvent event = new ClusterUpgradeValidationFinalizeEvent(payload.getResourceId());
                sendEvent(context, event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_FAILED_STATE")
    public Action<?, ?> clusterUpgradeValidationFailed() {
        return new AbstractAction<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors, ClusterUpgradeContext,
                ClusterUpgradeValidationFailureEvent>(ClusterUpgradeValidationFailureEvent.class) {

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters,
                    StateContext<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors> stateContext,
                    ClusterUpgradeValidationFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                StackView stackView = stackService.getViewByIdWithoutAuth(payload.getResourceId());
                MDCBuilder.buildMdcContext(stackView);
                flow.setFlowFailed(payload.getException());
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationFailureEvent payload, Map<Object, Object> variables) {
                String errorMessage = payload.getException().getMessage();
                Long resourceId = payload.getResourceId();
                LOGGER.debug("Cluster upgrade validation failed with validation error: {}", errorMessage);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FAILED,
                        Collections.singletonList(errorMessage));
                sendEvent(context, HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationFailureEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                return null;
            }

        };
    }

}
