package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_UPGRADE_PREPARATION_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_UPGRADE_PREPARATION_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DISTRIBUTE_PARCELS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_CM_PACKAGES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_CSD_PACKAGES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_PARCELS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FINALIZE_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.ClusterComponentUpdateService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ClusterUpgradePreparationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePreparationActions.class);

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ClusterComponentUpdateService clusterComponentUpdateService;

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_INIT_STATE")
    public Action<?, ?> initClusterUpgradePreparation() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Initiating cluster upgrade preparation {}", payload);
                Long resourceId = payload.getResourceId();
                ImageChangeDto imageChangeDto = payload.getImageChangeDto();
                List<String> parameterList = List.of(payload.getRuntimeVersion(), imageChangeDto.getImageId());
                ResourceEvent preparationStartedResourceEvent = ResourceEvent.CLUSTER_UPGRADE_PREPARATION_STARTED;
                stackUpdater.updateStackStatus(resourceId, CLUSTER_UPGRADE_PREPARATION_STARTED,
                        messagesService.getMessage(preparationStartedResourceEvent.getMessage(), parameterList));
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_IN_PROGRESS.name(), preparationStartedResourceEvent, parameterList);
                ClusterUpgradeParcelSettingsPreparationEvent nextEvent = new ClusterUpgradeParcelSettingsPreparationEvent(resourceId, imageChangeDto);
                sendEvent(context, nextEvent.selector(), nextEvent);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationTriggerEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation init state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CM_PACKAGES_STATE")
    public Action<?, ?> clusterUpgradePreparationCmPackageDownload() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Cluster upgrade preparation CM package download started.");
                String nextEvent = DOWNLOAD_CM_PACKAGES_EVENT.event();
                sendEvent(context, nextEvent, createClusterUpgradePreparationEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation CM package download state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_PARCEL_DOWNLOAD_STATE")
    public Action<?, ?> clusterUpgradePreparationParcelDownload() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Cluster upgrade preparation parcel download state started.");
                String nextEvent = DOWNLOAD_PARCELS_EVENT.event();
                sendEvent(context, nextEvent, createClusterUpgradePreparationEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation parcel download state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }

        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_PARCEL_DISTRIBUTION_STATE")
    public Action<?, ?> clusterUpgradePreparationParcelDistribution() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Cluster upgrade preparation parcel distribution state started.");
                String nextEvent = DISTRIBUTE_PARCELS_EVENT.event();
                sendEvent(context, nextEvent, createClusterUpgradePreparationEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation parcel distribution state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_DOWNLOAD_CSD_PACKAGES_STATE")
    public Action<?, ?> clusterUpgradePreparationCsdPackageDownload() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Cluster upgrade preparation CSD package download started.");
                String nextEvent = DOWNLOAD_CSD_PACKAGES_EVENT.event();
                sendEvent(context, nextEvent, createClusterUpgradePreparationEvent(nextEvent, payload));
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation CSD package download state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_FINISHED_STATE")
    public Action<?, ?> clusterUpgradePreparationFinished() {
        return new AbstractClusterUpgradePreparationAction<>(ClusterUpgradePreparationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradePreparationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Cluster upgrade preparation finish state started.");
                Long resourceId = payload.getResourceId();
                ResourceEvent resourceEvent = ResourceEvent.CLUSTER_UPGRADE_PREPARATION_FINISHED;
                try {
                    clusterComponentUpdateService.updateOrSavePreparedClusterComponent(resourceId, payload.getImageId());
                } catch (IOException ex) {
                    LOGGER.error("Unable to add {} to prepared list of images in ClusterComponent table.", payload.getImageId());
                }
                stackUpdater.updateStackStatus(resourceId, CLUSTER_UPGRADE_PREPARATION_FINISHED, messagesService.getMessage(resourceEvent.getMessage()));
                cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), resourceEvent);
                String nextEventSelector = FINALIZE_CLUSTER_UPGRADE_PREPARATION_EVENT.event();
                sendEvent(context, nextEventSelector, createClusterUpgradePreparationEvent(nextEventSelector, payload));
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("Cluster upgrade preparation finished state failed.", ex);
                return new ClusterUpgradePreparationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_PREPARATION_FAILED_STATE")
    public Action<?, ?> clusterUpgradePreparationFailed() {
        return new AbstractAction<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors, ClusterUpgradeContext,
                ClusterUpgradePreparationFailureEvent>(ClusterUpgradePreparationFailureEvent.class) {

            @Inject
            private StackService stackService;

            @Override
            protected ClusterUpgradeContext createFlowContext(FlowParameters flowParameters,
                    StateContext<ClusterUpgradePreparationState, ClusterUpgradePreparationStateSelectors> stateContext,
                    ClusterUpgradePreparationFailureEvent payload) {
                StackView stackView = stackService.getViewByIdWithoutAuth(payload.getResourceId());
                MDCBuilder.buildMdcContext(stackView);
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradePreparationFailureEvent payload, Map<Object, Object> variables) {
                String errorMessage = payload.getException().getMessage();
                Long resourceId = payload.getResourceId();
                LOGGER.error("Cluster upgrade preparation failed: {}", errorMessage, payload.getException());
                ResourceEvent preparationFailedResourceEvent = ResourceEvent.CLUSTER_UPGRADE_PREPARATION_FAILED;
                List<String> errorMessageAsList = singletonList(errorMessage);
                stackUpdater.updateStackStatus(resourceId, DetailedStackStatus.AVAILABLE,
                        messagesService.getMessage(preparationFailedResourceEvent.getMessage(), errorMessageAsList));
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(), preparationFailedResourceEvent, errorMessageAsList);
                sendEvent(context, HANDLED_FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradePreparationFailureEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                LOGGER.error("Error happened in cluster upgrade preparation failure state.", ex);
                return null;
            }
        };
    }

    private ClusterUpgradePreparationEvent createClusterUpgradePreparationEvent(String selector, ClusterUpgradePreparationEvent payload) {
        return new ClusterUpgradePreparationEvent(selector, payload.getResourceId(), payload.getClouderaManagerProducts(), payload.getImageId());
    }

}