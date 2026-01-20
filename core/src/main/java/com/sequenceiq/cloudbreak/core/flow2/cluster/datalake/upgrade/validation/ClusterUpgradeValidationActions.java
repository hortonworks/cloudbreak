package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLOUDPROVIDER_UPDATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_S3GUARD_VALIDATION_EVENT;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config.ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config.ClusterUpgradeUpdateCheckFailedToClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.config.ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeExistingUpgradeCommandValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeImageValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeUpdateCheckRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinalizeEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfoFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class ClusterUpgradeValidationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeValidationActions.class);

    private static final String LOCK_COMPONENTS = "lockComponents";

    private static final String ROLLING_UPGRADE_ENABLED = "rollingUpgradeEnabled";

    private static final String TARGET_IMAGE = "targetImage";

    private static final String UPGRADE_IMAGE_INFO = "upgradeImageInfo";

    private static final String TARGET_RUNTIME = "targetRuntime";

    private static final String REPLACE_VMS = "replaceVms";

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService eventService;

    @Bean(name = "CLUSTER_UPGRADE_VALIDATION_INIT_STATE")
    public Action<?, ?> initClusterUpgradeValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationTriggerEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting cluster upgrade validation flow. Target image: {}", payload.getImageId());
                ResourceEvent resourceEvent = ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED;
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_STARTED, getEventMessage(resourceEvent));
                cloudbreakEventService.fireCloudbreakEvent(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), resourceEvent);
                variables.put(LOCK_COMPONENTS, payload.isLockComponents());
                variables.put(ROLLING_UPGRADE_ENABLED, payload.isRollingUpgradeEnabled());
                variables.put(REPLACE_VMS, payload.isReplaceVms());
                ClusterUpgradeValidationEvent event = new ClusterUpgradeValidationEvent(START_CLUSTER_UPGRADE_S3GUARD_VALIDATION_EVENT.name(),
                        payload.getResourceId(), payload.getImageId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationTriggerEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_S3GUARD_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeS3guardValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeValidationEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeValidationEvent payload, Map<Object, Object> variables)
                    throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
                LOGGER.info("Starting S3guard validation. Target image: {}", payload.getImageId());
                UpgradeImageInfo upgradeImageInfo = upgradeImageInfoFactory.create(payload.getImageId(), payload.getResourceId());
                Image targetImage = stackImageService
                        .getImageModelFromStatedImage(context.getStack().getStack(), upgradeImageInfo.currentImage(),
                                upgradeImageInfo.targetStatedImage());
                variables.put(TARGET_IMAGE, targetImage);
                variables.put(UPGRADE_IMAGE_INFO, upgradeImageInfo);
                String targetVersion = targetImage.getPackageVersions().getOrDefault(ImagePackageVersion.STACK.getDisplayName(), "");
                variables.put(TARGET_RUNTIME, targetVersion);
                if (CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(targetVersion, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16)) {
                    ClusterUpgradeS3guardValidationEvent event = new ClusterUpgradeS3guardValidationEvent(payload.getResourceId(),
                            payload.getImageId());
                    sendEvent(context, event.selector(), event);
                } else {
                    ClusterUpgradeS3guardValidationFinishedEvent event = new ClusterUpgradeS3guardValidationFinishedEvent(
                            payload.getResourceId(), payload.getImageId());
                    sendEvent(context, event.selector(), event);
                }
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFinishedEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_IMAGE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeImageValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeS3guardValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeS3guardValidationFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting cluster upgrade image validation.");
                UpgradeImageInfo upgradeImageInfo = (UpgradeImageInfo) variables.get(UPGRADE_IMAGE_INFO);
                Image targetImage = (Image) variables.get(TARGET_IMAGE);
                CloudStack cloudStack = CloudStack.replaceImage(context.getCloudStack(), targetImage);
                ClusterUpgradeImageValidationEvent event = new ClusterUpgradeImageValidationEvent(payload.getResourceId(), payload.getImageId(), cloudStack,
                        context.getCloudCredential(), context.getCloudContext(), upgradeImageInfo.targetStatedImage().getImage());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeS3guardValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeDiskSpaceValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeImageValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeImageValidationFinishedEvent payload, Map<Object, Object> variables) {
                handleValidationWarnings(payload);
                LOGGER.info("Starting disk space validation.");
                ClusterUpgradeDiskSpaceValidationEvent event = new ClusterUpgradeDiskSpaceValidationEvent(VALIDATE_DISK_SPACE_EVENT.name(),
                        payload.getResourceId(), payload.getRequiredFreeSpace());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeImageValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeDiskSpaceValidationFinishedEvent(payload.getResourceId());
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<ClusterUpgradeImageValidationFinishedEvent>> payloadConverters) {
                payloadConverters.add(new ClusterUpgradeValidationEventToClusterUpgradeImageValidationFinishedEventConverter());
                payloadConverters.add(new ClusterUpgradeDiskSpaceValidationEventToClusterUpgradeImageValidationFinishedEventConverter());
            }

            private void handleValidationWarnings(ClusterUpgradeImageValidationFinishedEvent payload) {
                if (!CollectionUtils.isEmpty(payload.getWarningMessages())) {
                    eventService.fireCloudbreakEvent(payload.getResourceId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLOUD_PROVIDER_VALIDATION_WARNING,
                            payload.getWarningMessages());
                }
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_CLOUDPROVIDER_CHECK_UPDATE_STATE")
    public Action<?, ?> clusterUpgradeCheckCloudProviderUpdate() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeDiskSpaceValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeDiskSpaceValidationFinishedEvent payload, Map<Object, Object> variables) {
                Collection<Resource> resources = resourceService.getAllByStackId(context.getStack().getId());
                List<CloudResource> cloudResources = resources.stream()
                        .map(resource -> resourceToCloudResourceConverter.convert(resource))
                        .collect(Collectors.toList());
                ClusterUpgradeUpdateCheckRequest clusterUpgradeUpdateCheckRequest = new ClusterUpgradeUpdateCheckRequest(
                        payload.getResourceId(), context.getCloudStack(), context.getCloudCredential(), context.getCloudContext(), cloudResources);
                sendEvent(context, VALIDATE_CLOUDPROVIDER_UPDATE.selector(), clusterUpgradeUpdateCheckRequest);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeDiskSpaceValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_EXISTING_UPGRADE_COMMAND_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeExistingUpgradeCommandValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeUpdateCheckFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeUpdateCheckFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting the validation if an existing, retryable upgradeCDH command exists...");
                Image targetImage = (Image) variables.get(TARGET_IMAGE);
                ClusterUpgradeExistingUpgradeCommandValidationEvent event =
                        new ClusterUpgradeExistingUpgradeCommandValidationEvent(payload.getResourceId(), targetImage);
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeUpdateCheckFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_FREEIPA_STATUS_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeFreeIpaStatusValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting the validation if FreeIPA is reachable...");
                ClusterUpgradeFreeIpaStatusValidationEvent event = new ClusterUpgradeFreeIpaStatusValidationEvent(payload.getResourceId());
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeExistingUpgradeCommandValidationFinishedEvent payload,
                    Optional<StackContext> flowContext, Exception ex) {
                return new ClusterUpgradeValidationFailureEvent(payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "CLUSTER_UPGRADE_SERVICE_VALIDATION_STATE")
    public Action<?, ?> clusterUpgradeServiceValidation() {
        return new AbstractClusterUpgradeValidationAction<>(ClusterUpgradeFreeIpaStatusValidationFinishedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ClusterUpgradeFreeIpaStatusValidationFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting to validate services.");
                boolean lockComponents = (Boolean) variables.get(LOCK_COMPONENTS);
                boolean rollingUpgradeEnabled = Optional.ofNullable(variables.get(ROLLING_UPGRADE_ENABLED)).map(variable -> (Boolean) (variable)).orElse(false);
                boolean replaceVms = Optional.ofNullable(variables.get(REPLACE_VMS)).map(variable -> (Boolean) (variable)).orElse(false);
                String targetRuntime = (String) variables.get(TARGET_RUNTIME);
                UpgradeImageInfo imageInfo = (UpgradeImageInfo) variables.get(UPGRADE_IMAGE_INFO);
                ClusterUpgradeServiceValidationEvent event = new ClusterUpgradeServiceValidationEvent(payload.getResourceId(), lockComponents,
                        rollingUpgradeEnabled, targetRuntime, imageInfo, replaceVms);
                sendEvent(context, event.selector(), event);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeFreeIpaStatusValidationFinishedEvent payload, Optional<StackContext> flowContext, Exception ex) {
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
                    ResourceEvent resourceEvent = ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FINISHED;
                    stackUpdater.updateStackStatus(resourceId, DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_FINISHED, getEventMessage(resourceEvent));
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), resourceEvent);
                } else {
                    LOGGER.info("Cluster upgrade validation finished due to an error", exception);
                    ResourceEvent resourceEvent = ResourceEvent.CLUSTER_UPGRADE_VALIDATION_SKIPPED;
                    List<String> errorMessageAsList = Collections.singletonList(exception.getMessage());
                    String reason = messagesService.getMessage(resourceEvent.getMessage(), errorMessageAsList);
                    stackUpdater.updateStackStatus(resourceId, DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_SKIPPED, reason);
                    cloudbreakEventService.fireCloudbreakEvent(resourceId, AVAILABLE.name(), resourceEvent, errorMessageAsList);
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
                StackView stackView = stackService.getViewByIdWithoutAuth(payload.getResourceId());
                MDCBuilder.buildMdcContext(stackView);
                return ClusterUpgradeContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(ClusterUpgradeContext context, ClusterUpgradeValidationFailureEvent payload, Map<Object, Object> variables) {
                String errorMessage = payload.getException().getMessage();
                Long resourceId = payload.getResourceId();
                LOGGER.debug("Cluster upgrade validation failed with validation error: {}", errorMessage);
                ResourceEvent validationFailedResourceEvent = ResourceEvent.CLUSTER_UPGRADE_VALIDATION_FAILED;
                List<String> errorMessageAsList = Collections.singletonList(errorMessage);
                String reason = messagesService.getMessage(validationFailedResourceEvent.getMessage(), errorMessageAsList);
                stackUpdater.updateStackStatus(resourceId, DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_FAILED, reason);
                cloudbreakEventService.fireCloudbreakEvent(resourceId, UPDATE_FAILED.name(), validationFailedResourceEvent, errorMessageAsList);
                sendEvent(context, HANDLED_FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ClusterUpgradeValidationFailureEvent payload, Optional<ClusterUpgradeContext> flowContext, Exception ex) {
                LOGGER.warn("No failure payload in case of CLUSTER_UPGRADE_VALIDATION_FAILED_STATE. This should not happen.", ex);
                return null;
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<ClusterUpgradeValidationFailureEvent>> payloadConverters) {
                payloadConverters.add(new ClusterUpgradeUpdateCheckFailedToClusterUpgradeValidationFailureEvent());
            }
        };
    }

    private String getEventMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }
}
