package com.sequenceiq.cloudbreak.core.flow2.stack.provision.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.PROVISION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_FALLBACK;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_MARKETPLACE_ERROR;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_SETUP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_ROLLBACK_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_TIME;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_PROVISIONING;
import static com.sequenceiq.cloudbreak.util.EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned;
import static com.sequenceiq.common.api.type.ImageStatus.CREATE_FAILED;
import static com.sequenceiq.common.api.type.ImageStatus.CREATE_FINISHED;
import static java.lang.String.format;
import static java.util.EnumSet.of;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.loadbalancer.CollectLoadBalancerMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchLoadBalancerResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupEphemeralVolumeChecker;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.notification.WebSocketNotification;
import com.sequenceiq.notification.WebSocketNotificationService;

@Component
public class StackCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationService.class);

    private static final String IMAGE_COPY_START_MILLIS = "IMAGE_COPY_START_MILLIS";

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ImageService imageService;

    @Inject
    private WebSocketNotificationService webSocketNotificationService;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventBus eventBus;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private MetadataSetupService metadatSetupService;

    @Inject
    private TlsSetupService tlsSetupService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private CloudbreakMetricService metricService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private TemplateService templateService;

    @Inject
    private InstanceGroupEphemeralVolumeChecker ephemeralVolumeChecker;

    public void setupProvision(Long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISION_SETUP, "Provisioning setup");
    }

    public void prepareImage(Long stackId, Map<Object, Object> variables) {
        variables.put(IMAGE_COPY_START_MILLIS, System.currentTimeMillis());
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.IMAGE_SETUP, "Image setup");
        flowMessageService.fireEventAndLog(stackId, CREATE_IN_PROGRESS.name(), STACK_IMAGE_SETUP);
    }

    public void startProvisioning(StackDto stack, CloudStack cloudStack, Map<Object, Object> variables) {
        if (variables.containsKey(IMAGE_COPY_START_MILLIS)) {
            long startMillis = (long) variables.get(IMAGE_COPY_START_MILLIS);
            metricService.recordImageCopyTime(stack, startMillis);
        }
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATING_INFRASTRUCTURE, "Creating infrastructure");
        flowMessageService.fireEventAndLog(stack.getId(), CREATE_IN_PROGRESS.name(), STACK_PROVISIONING);
        instanceMetaDataService.saveInstanceRequests(stack, cloudStack.getGroups());
    }

    public void fireImageFallbackFlowMessage(Long stackId, String notificationMessage) {
        flowMessageService.fireEventAndLog(stackId, CREATE_IN_PROGRESS.name(), STACK_IMAGE_FALLBACK);
        flowMessageService.fireEventAndLog(stackId, CREATE_IN_PROGRESS.name(), STACK_IMAGE_MARKETPLACE_ERROR, notificationMessage);
    }

    public Stack stackProvisioningFinished(StackContext context, LaunchStackResult result, Map<Object, Object> variables) {
        Date startDate = getStartDateIfExist(variables);
        StackDtoDelegate stack = context.getStack();
        validateResourceResults(context.getCloudContext(), result);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATING_LOAD_BALANCER, "Creating load balancer");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_TIME,
                String.valueOf(calculateStackCreationTime(startDate)));
        Stack provisionedStack = stackService.getByIdWithListsInTransaction(stack.getId());
        provisionedStack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        return provisionedStack;
    }

    public void loadBalancerProvisioningFinished(StackCreationContext context, LaunchLoadBalancerResult result, Map<Object, Object> variables) {
        validateResourceResults(context.getCloudContext(), result);
        stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.METADATA_COLLECTION, "Metadata collection");
    }

    private Date getStartDateIfExist(Map<Object, Object> variables) {
        Date result = null;
        Object startDateObj = variables.get(START_DATE);
        if (startDateObj instanceof Date) {
            result = (Date) startDateObj;
        }
        return result;
    }

    public CheckImageResult checkImage(StackCreationContext context) {
        try {
            StackDto stack = stackDtoService.getById(context.getStackId());
            Image image = imageService.getImage(stack.getId());
            CheckImageRequest<CheckImageResult> checkImageRequest = new CheckImageRequest<>(context.getCloudContext(), context.getCloudCredential(),
                    cloudStackConverter.convert(stack), image);
            LOGGER.debug("Triggering event: {}", checkImageRequest);
            eventBus.notify(checkImageRequest.selector(), eventFactory.createEvent(checkImageRequest));
            CheckImageResult result = checkImageRequest.await();
            sendNotificationIfNecessary(result, stack);
            LOGGER.debug("Result: {}", result);
            return result;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing check image", e);
            Thread.currentThread().interrupt();
            throw new OperationException(e);
        } catch (CloudbreakImageNotFoundException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    public void setupMetadata(StackView stack, CollectMetadataResult collectMetadataResult) {
        metadatSetupService.saveInstanceMetaData(stack, collectMetadataResult.getResults(), InstanceStatus.CREATED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.LOAD_BALANCER_METADATA_COLLECTION, "Load balancer metadata collection");
        LOGGER.debug("Metadata setup DONE.");
    }

    public void setupLoadBalancerMetadata(StackView stack, CollectLoadBalancerMetadataResult collectMetadataResult) {
        metadatSetupService.saveLoadBalancerMetadata(stack, collectMetadataResult.getResults());
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.TLS_SETUP, "TLS setup");
        LOGGER.debug("Load balancer metadata setup DONE.");
    }

    public void saveTlsInfo(SecurityConfig securityConfig, TlsInfo tlsInfo) {
        boolean usePrivateIpToTls = tlsInfo.isUsePrivateIpToTls();
        if (usePrivateIpToTls) {
            securityConfig.setUsePrivateIpToTls(true);
            stackUpdater.updateSecurityConfig(securityConfig);
            LOGGER.debug("Update Stack and it's SecurityConfig to use private ip when TLS is built.");
        }
    }

    public void setupTls(StackDto stackDto) throws CloudbreakException {
        for (InstanceMetadataView gwInstance : stackDto.getAllAvailableGatewayInstances()) {
            tlsSetupService.setupTls(stackDto, gwInstance);
        }
    }

    public void stackCreationFinished(Long stackId) {
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.PROVISIONED, "Stack provisioned.");
    }

    public void handleStackCreationFailure(StackDto stack, Exception errorDetails, ProvisionType provisionType) {
        LOGGER.info("Error during stack creation flow:", errorDetails);
        String errorReason = errorDetails == null ? "Unknown error" : errorDetails.getMessage();
        if (errorDetails instanceof CancellationException || ExceptionUtils.getRootCause(errorDetails) instanceof CancellationException) {
            LOGGER.debug("The flow has been cancelled.");
        } else {
            if (!stack.isStackInDeletionPhase()) {
                handleFailure(stack, errorReason);
                DetailedStackStatus failureStatus = (provisionType == ProvisionType.REGULAR) ? PROVISION_FAILED : CLUSTER_RECOVERY_FAILED;
                stackUpdater.updateStackStatus(stack.getId(), failureStatus, errorReason);
                flowMessageService.fireEventAndLog(stack.getId(), Status.CREATE_FAILED.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            } else {
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            }
        }
    }

    public void setInstanceStoreCount(StackCreationContext stackContext) {
        StackView stack = stackContext.getStack();
        List<InstanceGroupDto> instanceGroupDtos = stackDtoService.getInstanceMetadataByInstanceGroup(stack.getId());
        CloudConnector connector = cloudPlatformConnectors.get(stackContext.getCloudContext().getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(stackContext.getCloudContext(), stackContext.getCloudCredential());

        List<String> instanceTypes = instanceGroupDtos.stream()
                .map(ig -> ig.getInstanceGroup().getTemplate())
                .filter(Objects::nonNull)
                .map(Template::getInstanceType)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        InstanceStoreMetadata instanceStoreMetadata = connector.metadata().collectInstanceStorageCount(ac, instanceTypes);


        for (InstanceGroupDto ig : instanceGroupDtos) {
            InstanceGroupView instanceGroup = ig.getInstanceGroup();
            Template template = instanceGroup.getTemplate();
            if (template != null) {
                Integer instanceStorageCount = instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled(template.getInstanceType());
                Integer instanceStorageSize = instanceStoreMetadata.mapInstanceTypeToInstanceSizeNullHandled(template.getInstanceType());
                if (ephemeralVolumeChecker.instanceGroupContainsOnlyDatabaseAndEphemeralVolumes(instanceGroup)) {
                    LOGGER.debug("Instance storage was already requested. Setting temporary storage in template to: {}. " +
                                    "Group name: {}, Template id: {}, instance type: {}",
                            TemporaryStorage.EPHEMERAL_VOLUMES_ONLY.name(), instanceGroup.getGroupName(), template.getId(), template.getInstanceType());
                    template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES_ONLY);
                } else if ((hasDiskWhichIsEphemeralAndProvisionedByCloudbreak(template) || instanceStorageCount > 0)
                        && stack.getType().equals(StackType.WORKLOAD)) {
                    LOGGER.debug("The host group's instance type has ephemeral volumes. Setting temporary storage in template to: {}. " +
                                    "Group name: {}, Template id: {}, instance type: {}",
                            TemporaryStorage.EPHEMERAL_VOLUMES.name(), instanceGroup.getGroupName(), template.getId(), template.getInstanceType());
                    template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);
                }
                LOGGER.debug("Setting instance storage count in template. " +
                        "Group name: {}, Template id: {}, instance type: {}", instanceGroup.getGroupName(), template.getId(), template.getInstanceType());
                template.setInstanceStorageCount(instanceStorageCount);
                template.setInstanceStorageSize(instanceStorageSize);
                templateService.savePure(template);
            }
        }
    }

    private boolean hasDiskWhichIsEphemeralAndProvisionedByCloudbreak(Template template) {
        // AWS ephemeral storages are not provisioned by CB this is why google is different
        return template.getVolumeTemplates()
                .stream()
                .anyMatch(e -> volumeIsEphemeralWhichMustBeProvisioned(e));
    }

    private void sendNotificationIfNecessary(CheckImageResult result, StackDtoDelegate stack) {
        if (of(CREATE_FAILED, CREATE_FINISHED).contains(result.getImageStatus())) {
            webSocketNotificationService.send(getImageCopyNotification(result, stack));
        }
    }

    private WebSocketNotification<CloudbreakEventV4Response> getImageCopyNotification(CheckImageResult result, StackDtoDelegate stack) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventType("IMAGE_COPY_STATE");
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(String.valueOf(result.getStatusProgressValue()));
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspaceId());
        notification.setCloud(stack.getCloudPlatform());
        notification.setRegion(stack.getRegion());
        notification.setStackCrn(stack.getResourceCrn());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        notification.setTenantName(stack.getCreator().getTenant().getName());
        return new WebSocketNotification<>(notification);
    }

    private void handleFailure(StackDto stack, String errorReason) {
        try {
            if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
            } else {
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ROLLING_BACK, errorReason);
                connector.rollback(stack);
                flowMessageService.fireEventAndLog(stack.getId(), Status.CREATE_FAILED.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            }
        } catch (Exception ex) {
            LOGGER.info("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), PROVISION_FAILED, format("Rollback failed: %s", ex.getMessage()));
            flowMessageService.fireEventAndLog(stack.getId(), Status.CREATE_FAILED.name(), STACK_INFRASTRUCTURE_ROLLBACK_FAILED, ex.getMessage());
        }
    }

    private long calculateStackCreationTime(Date startDate) {
        long result = 0;
        if (startDate != null) {
            return (new Date().getTime() - startDate.getTime()) / DateUtils.MILLIS_PER_SECOND;
        }
        return result;
    }

    private void validateResourceResults(CloudContext cloudContext, LaunchStackResult res) {
        validateResourceResults(cloudContext, res.getErrorDetails(), res.getResults());
    }

    private void validateResourceResults(CloudContext cloudContext, LaunchLoadBalancerResult res) {
        validateResourceResults(cloudContext, res.getErrorDetails(), res.getResults());
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results) {
        String action = "create";
        if (exception != null) {
            LOGGER.info(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results.get(0).getStatusReason()));
        }
        List<CloudResourceStatus> failedResources = results.stream().filter(r -> r.isFailed() || r.isDeleted()).collect(Collectors.toList());
        if (!failedResources.isEmpty()) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, failedResources));
        }
    }
}