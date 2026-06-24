package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_PREPARE_UPGRADE_STARTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CLUSTER_OPERATION;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_IMAGE_COPY_CHECK_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_IMAGE_COPY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_IMAGE_FALLBACK_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.PrepareUpgradeEvent.PREPARE_UPGRADE_UPDATE_IMAGE_PARAMETER_FINISHED_EVENT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
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
import com.sequenceiq.freeipa.flow.stack.provision.PrepareImageResultToStackEventConverter;
import com.sequenceiq.freeipa.flow.stack.provision.action.CheckImageAction;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerProvisionCondition;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class PrepareUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareUpgradeActions.class);

    private static final String TEST_LB_CREATED = "TEST_LB_CREATED";

    private static final String FAILURE_EXCEPTION = "FAILURE_EXCEPTION";

    private static final String PREPARE_UPGRADE_TRIGGER_EVENT = "PREPARE_UPGRADE_TRIGGER_EVENT";

    private static final String IMAGE = "IMAGE";

    private static final String FALLBACK_IMAGE_NAME = "FALLBACK_IMAGE_NAME";

    private static final String IMAGE_IDENTIFIER_PARAMETER = "IMAGE_IDENTIFIER_PARAMETER";

    private static final String MISSING_SECURITY_GROUPS_ERROR = "Upgrade cannot start: security group(s) %s referenced "
            + "in FreeIPA metadata do not exist. Update security group metadata or restore the groups before retrying.";

    private static final String VPC_MISMATCH_ERROR = "Upgrade cannot start: security group(s) %s referenced in FreeIPA "
            + "metadata do not belong to the environment VPC. Update security group metadata before retrying.";

    private static final String VALIDATION_IN_PROGRESS_REASON = "Validating security groups before upgrade";

    @Bean(name = "PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_STATE")
    public Action<?, ?> prepareUpgradeSecurityGroupValidation() {
        return new AbstractPrepareUpgradeAction<>(PrepareUpgradeTriggerEvent.class) {

            @Inject
            private PrepareUpgradeSecurityGroupValidationRequestProvider requestProvider;

            @Inject
            private CredentialService credentialService;

            @Inject
            private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

            @Override
            protected void prepareExecution(PrepareUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
                variables.put(PREPARE_UPGRADE_TRIGGER_EVENT, payload);
            }

            @Override
            protected void doExecute(StackContext context, PrepareUpgradeTriggerEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                Stack stack = context.getStack();
                getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), FREEIPA_PREPARE_UPGRADE_STARTED);
                if (!payload.isNeedMigration() || !CloudPlatform.AWS.name().equals(stack.getCloudPlatform())) {
                    LOGGER.debug("Skipping security group validation for stack {} (needMigration={}, platform={})",
                            stackId, payload.isNeedMigration(), stack.getCloudPlatform());
                    sendEvent(context, PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT.event(),
                            new SecurityGroupValidationResult(stackId, Set.of(), Set.of()));
                } else {
                    Set<String> securityGroupIds = requestProvider.collectSecurityGroupIds(stack);
                    if (securityGroupIds.isEmpty()) {
                        LOGGER.debug("No security group IDs to validate for FreeIPA stack {}, short-circuiting SG validation", stackId);
                        sendEvent(context, PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT.event(),
                                new SecurityGroupValidationResult(stackId, Set.of(), Set.of()));
                    } else {
                        stackUpdater().updateStackStatus(stack, CLUSTER_OPERATION, VALIDATION_IN_PROGRESS_REASON);
                        String vpcId = requestProvider.resolveAwsVpcId(stack.getEnvironmentCrn());
                        ExtendedCloudCredential extendedCloudCredential = credentialToExtendedCloudCredentialConverter.convert(
                                credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
                        SecurityGroupValidationRequest request = new SecurityGroupValidationRequest(
                                context.getCloudContext(),
                                context.getCloudCredential(),
                                extendedCloudCredential,
                                context.getCloudContext().getLocation().getRegion().getRegionName(),
                                securityGroupIds,
                                vpcId);
                        sendEvent(context, request.selector(), request);
                    }
                }
            }

            @Override
            protected Object getFailurePayload(PrepareUpgradeTriggerEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new SecurityGroupValidationResult(ex.getMessage(), ex, payload.getResourceId());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_RESULT_STATE")
    public Action<?, ?> prepareUpgradeSecurityGroupValidationResult() {
        return new AbstractPrepareUpgradeAction<>(SecurityGroupValidationResult.class) {

            @Override
            protected void doExecute(StackContext context, SecurityGroupValidationResult payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                if (payload.getStatus() == EventStatus.FAILED) {
                    stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.UPGRADE_VALIDATION_FAILED,
                            payload.getStatusReason() != null ? payload.getStatusReason() : "Security group validation failed");
                    sendEvent(context, PREPARE_UPGRADE_FAILURE_EVENT.event(), new PrepareUpgradeFailureEvent(stackId, VALIDATION,
                            new CloudbreakServiceException(payload.getStatusReason() != null
                                    ? payload.getStatusReason()
                                    : "Security group validation failed")));
                } else {
                    Set<String> missing = payload.getMissingSecurityGroupIds();
                    Set<String> notInNetwork = payload.getNotInNetworkSecurityGroupIds();
                    if (!missing.isEmpty()) {
                        LOGGER.warn("Prepare upgrade SG validation failed for FreeIPA stack {} due to missing security groups: {}", stackId, missing);
                        String errorReason = String.format(MISSING_SECURITY_GROUPS_ERROR, formatSecurityGroupIds(missing));
                        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.UPGRADE_VALIDATION_FAILED, errorReason);
                        sendEvent(context, PREPARE_UPGRADE_FAILURE_EVENT.event(),
                                new PrepareUpgradeFailureEvent(stackId, VALIDATION, new CloudbreakServiceException(errorReason)));
                    } else if (!notInNetwork.isEmpty()) {
                        LOGGER.warn("Prepare upgrade SG validation failed for FreeIPA stack {} due to VPC mismatch: {}", stackId, notInNetwork);
                        String errorReason = String.format(VPC_MISMATCH_ERROR, formatSecurityGroupIds(notInNetwork));
                        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.UPGRADE_VALIDATION_FAILED, errorReason);
                        sendEvent(context, PREPARE_UPGRADE_FAILURE_EVENT.event(),
                                new PrepareUpgradeFailureEvent(stackId, VALIDATION, new CloudbreakServiceException(errorReason)));
                    } else {
                        sendEvent(context, new StackEvent(PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINALIZED_EVENT.event(), stackId));
                    }
                }
            }

            private String formatSecurityGroupIds(Set<String> securityGroupIds) {
                return securityGroupIds.stream().sorted().collect(Collectors.joining(", ", "[", "]"));
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_PREPARE_IMAGE_STATE")
    public AbstractPrepareUpgradeAction<?> prepareImage() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {
            @Inject
            private ImageService imageService;

            @Inject
            private ImageFallbackService imageFallbackService;

            @Inject
            private ImageConverter imageConverter;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                PrepareUpgradeTriggerEvent trigger = (PrepareUpgradeTriggerEvent) variables.get(PREPARE_UPGRADE_TRIGGER_EVENT);
                Stack stack = context.getStack();
                getStackUpdater().updateStackStatus(stack, CLUSTER_OPERATION, "Preparing image on cloud provider side");
                CloudContext cloudContext = context.getCloudContext();

                Pair<ImageWrapper, String> imageWrapperAndName = imageService.fetchImageWrapperAndName(stack, trigger.getImageSettingsRequest());
                String regionName = cloudContext.getLocation().getRegion().value();
                String platform = cloudContext.getPlatform().getValue();
                String fallbackImageName = null;
                if (imageFallbackService.imageFallbackPermitted(imageWrapperAndName.getRight(), stack)) {
                    try {
                        fallbackImageName = imageService.determineImageNameByRegion(platform, regionName, imageWrapperAndName.getLeft().getImage());
                        variables.put(FALLBACK_IMAGE_NAME, fallbackImageName);
                    } catch (ImageNotFoundException e) {
                        LOGGER.warn("Fallback image could not be determined due to exception {}," +
                                " we should continue execution", e.getMessage());
                    }
                }

                Image image = imageConverter.convert(imageWrapperAndName);
                variables.put(IMAGE, image);
                CloudStack cloudStack = context.getCloudStack().toBuilder().image(image).build();
                PrepareImageRequest<Object> request = new PrepareImageRequest<>(cloudContext, context.getCloudCredential(), cloudStack, image,
                        PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, fallbackImageName);
                LOGGER.info("Prepare image: {}, fallback image:{}", image, fallbackImageName);
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new PrepareImageResult(ex, payload.getResourceId());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_SET_FALLBACK_IMAGE_STATE")
    public AbstractPrepareUpgradeAction<?> imageFallbackAction() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {

            @Inject
            private ImageFallbackService imageFallbackService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                Image image = (Image) variables.get(IMAGE);
                String fallbackImageName = (String) variables.get(FALLBACK_IMAGE_NAME);
                if (fallbackImageName != null && imageFallbackService.imageFallbackPermitted(image.getImageName(), stack)) {
                    LOGGER.info("Falling back from image '{}' to VHD image '{}' during prepare upgrade", image.getImageName(), fallbackImageName);
                    getStackUpdater().updateStackStatus(stack, DetailedStackStatus.CLUSTER_OPERATION, "Setting up fallback image");
                    Image fallbackImage = Image.builder().withImage(image).withImageName(fallbackImageName).build();
                    variables.put(IMAGE, fallbackImage);
                } else {
                    LOGGER.info("Image fallback not required for image '{}' during prepare upgrade, continuing with the original image",
                            image == null ? null : image.getImageName());
                }
                ImageFallbackSuccess imageFallbackSuccess = new ImageFallbackSuccess(stack.getId());
                sendEvent(context, PREPARE_UPGRADE_IMAGE_FALLBACK_FINISHED_EVENT.event(), imageFallbackSuccess);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new PrepareImageResultToStackEventConverter());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_UPDATE_IMAGE_PARAMETER_STATE")
    public Action<?, ?> updateImageParameterAction() {
        return new AbstractPrepareUpgradeAction<>(PrepareImageResult.class) {

            @Override
            protected void doExecute(StackContext context, PrepareImageResult payload, Map<Object, Object> variables) {
                if (StringUtils.isNotBlank(payload.getImageIdentifier())) {
                    LOGGER.info("Storing prepared image identifier '{}' for the image copy check step", payload.getImageIdentifier());
                    variables.put(IMAGE_IDENTIFIER_PARAMETER, payload.getImageIdentifier());
                } else {
                    LOGGER.debug("No image identifier was returned by the prepare image step, continuing without it");
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(PREPARE_UPGRADE_UPDATE_IMAGE_PARAMETER_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_CHECK_IMAGE_STATE")
    public Action<?, ?> checkImageAction() {
        return new CheckImageAction() {

            @Inject
            private ErrorHandlerAwareReactorEventFactory eventFactory;

            @Inject
            private EventBus eventBus;

            @Override
            protected CheckImageResult checkImage(StackContext context, Map<Object, Object> variables) {
                CloudStack cloudStack = context.getCloudStack();
                CloudStack.Builder builder = cloudStack.toBuilder();
                Image image = (Image) variables.get(IMAGE);
                builder.image(image);
                Optional.ofNullable(variables.get(IMAGE_IDENTIFIER_PARAMETER)).ifPresent(identifier -> {
                    Map<String, String> parameters = new HashMap<>(cloudStack.getParameters());
                    parameters.put(PlatformParametersConsts.IMAGE_IDENTIFIER, (String) identifier);
                    builder.parameters(parameters);
                });
                CloudStack updatedStack = builder.build();

                try {
                    CheckImageRequest<CheckImageResult> checkImageRequest = new CheckImageRequest<>(context.getCloudContext(), context.getCloudCredential(),
                            updatedStack, image);
                    LOGGER.debug("Triggering event: {}", checkImageRequest);
                    eventBus.notify(checkImageRequest.selector(), eventFactory.createEvent(checkImageRequest));
                    CheckImageResult result = checkImageRequest.await();
                    LOGGER.debug("Result: {}", result);
                    return result;
                } catch (InterruptedException e) {
                    LOGGER.error("Error while executing check image", e);
                    throw new OperationException(e);
                } catch (Exception e) {
                    throw new CloudbreakServiceException(e);
                }
            }

            @Override
            protected FlowEvent getFinishedEvent() {
                return PREPARE_UPGRADE_IMAGE_COPY_FINISHED_EVENT;
            }

            @Override
            protected FlowEvent getRepeatEvent() {
                return PREPARE_UPGRADE_IMAGE_COPY_CHECK_EVENT;
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return new PrepareUpgradeFailureEvent(payload.getResourceId(), VALIDATION, ex);
            }
        };
    }

    @Bean(name = "PREPARE_UPGRADE_LB_CONFIGURATION_STATE")
    public Action<?, ?> prepareUpgradeLbConfiguration() {
        return new AbstractPrepareUpgradeAction<>(StackEvent.class) {

            @Inject
            private FreeIpaLoadBalancerConfigurationService freeIpaLoadBalancerConfigurationService;

            @Inject
            private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

            @Inject
            private FreeIpaLoadBalancerProvisionCondition freeIpaLoadBalancerProvisionCondition;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                if (!CloudPlatform.AWS.name().equals(context.getStack().getCloudPlatform())) {
                    LOGGER.debug("Stack is not on AWS, skipping prepare upgrade LB validation");
                    sendEvent(context, new StackEvent(PREPARE_UPGRADE_FINISHED_EVENT.event(), stackId));
                } else if (freeIpaLoadBalancerService.findByStackId(stackId).isPresent()) {
                    LOGGER.debug("LoadBalancer already exists for stack, permission already validated");
                    sendEvent(context, new StackEvent(PREPARE_UPGRADE_FINISHED_EVENT.event(), stackId));
                } else if (!freeIpaLoadBalancerProvisionCondition.loadBalancerProvisionEnabled(stackId, FreeIpaLoadBalancerType.INTERNAL_NLB)) {
                    LOGGER.debug("LoadBalancer creation is not enabled for stack, skipping LB creation");
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

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<PrepareUpgradeFailureEvent>> payloadConverters) {
                payloadConverters.add(new PrepareImageResultToPrepareUpgradeFailureConverter());
                payloadConverters.add(new SecurityGroupValidationResultToPrepareUpgradeFailureConverter());
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

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new SecurityGroupValidationResultToStackEventConverter());
            }
        };
    }
}
