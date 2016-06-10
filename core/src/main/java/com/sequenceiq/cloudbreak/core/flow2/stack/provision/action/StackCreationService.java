package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.type.BillingStatus.BILLING_STOPPED;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.notification.Notification;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StackCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationService.class);

    @Inject
    private StackService stackService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ImageService imageService;
    @Inject
    private NotificationSender notificationSender;
    @Inject
    private EventBus eventBus;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private ServiceProviderConnectorAdapter connector;
    @Inject
    private InstanceGroupRepository instanceGroupRepository;
    @Inject
    private InstanceMetadataService instanceMetadataService;
    @Inject
    private MetadataSetupService metadatSetupService;
    @Inject
    private TlsSetupService tlsSetupService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private FlowMessageService flowMessageService;

    public void startProvisioning(StackContext context) {
        Stack stack = context.getStack();
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), CREATE_IN_PROGRESS, "Creating infrastructure");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_PROVISIONING, CREATE_IN_PROGRESS.name());
        instanceMetadataService.saveInstanceRequests(stack, context.getCloudStack().getGroups());
    }

    public Stack provisioningFinished(StackContext context, LaunchStackResult result, Map<Object, Object> variables) {
        Date startDate = getStartDateIfExist(variables);
        Stack stack = context.getStack();
        validateResourceResults(context.getCloudContext(), result);
        List<CloudResourceStatus> results = result.getResults();
        updateNodeCount(stack.getId(), context.getCloudStack().getGroups(), results, true);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_TIME, UPDATE_IN_PROGRESS.name(), calculateStackCreationTime(startDate));
        return stackService.getById(stack.getId());
    }

    private Date getStartDateIfExist(Map<Object, Object> variables) {
        Date result = null;
        Object startDateObj = variables.get(START_DATE);
        if (startDateObj != null && startDateObj instanceof Date) {
            result = (Date) startDateObj;
        }
        return result;
    }

    public CheckImageResult checkImage(StackContext context) {
        Stack stack = context.getStack();
        Image image = imageService.getImage(stack.getId());
        CheckImageRequest<CheckImageResult> checkImageRequest = new CheckImageRequest<>(context.getCloudContext(), context.getCloudCredential(),
                cloudStackConverter.convert(stack), image);
        LOGGER.info("Triggering event: {}", checkImageRequest);
        eventBus.notify(checkImageRequest.selector(), Event.wrap(checkImageRequest));
        try {
            CheckImageResult result = checkImageRequest.await();
            sendNotification(result, stack);
            LOGGER.info("Result: {}", result);
            return result;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing check image", e);
            throw new OperationException(e);
        }
    }

    public Stack setupMetadata(StackContext context, CollectMetadataResult collectMetadataResult) {
        Stack stack = context.getStack();
        metadatSetupService.saveInstanceMetaData(stack, collectMetadataResult.getResults(), InstanceStatus.CREATED);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.FLOW_STACK_PROVISIONED, BillingStatus.BILLING_STARTED.name());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.FLOW_STACK_METADATA_COLLECTED, AVAILABLE.name());
        LOGGER.debug("Metadata setup DONE.");
        return stackService.getById(stack.getId());
    }

    public Stack setupTls(StackContext context, GetSSHFingerprintsResult sshFingerprints) throws CloudbreakException {
        LOGGER.info("Fingerprint has been determined: {}", sshFingerprints.getSshFingerprints());
        Stack stack = context.getStack();
        InstanceMetaData firstInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        tlsSetupService.setupTls(stack, stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next().getPublicIpWrapper(),
                firstInstance.getSshPort(), stack.getCredential().getLoginUserName(), sshFingerprints.getSshFingerprints());
        return stackService.getById(stack.getId());
    }

    public void bootstrappingMachines(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_BOOTSTRAP, UPDATE_IN_PROGRESS.name());
    }

    public void collectingHostMetadata(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_METADATA_SETUP, UPDATE_IN_PROGRESS.name());
    }

    public void stackCreationFinished(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
    }

    public void handleStackCreationFailure(Stack stack, Exception errorDetails) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.error("Error during stack creation flow:", errorDetails);
        String errorReason = errorDetails == null ? "Unknown error" : errorDetails.getMessage();
        if (errorDetails instanceof CancellationException || ExceptionUtils.getRootCause(errorDetails) instanceof CancellationException) {
            LOGGER.warn("The flow has been cancelled.");
        } else {
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, UPDATE_IN_PROGRESS.name(), errorReason);
            if (!stack.isStackInDeletionPhase()) {
                handleFailure(stack, errorReason);
                stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, errorReason);
                flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, CREATE_FAILED.name(), errorReason);
            }
        }
    }

    private void sendNotification(CheckImageResult result, Stack stack) {
        notificationSender.send(getImageCopyNotification(result, stack));
    }

    private Notification getImageCopyNotification(CheckImageResult result, Stack stack) {
        Notification notification = new Notification();
        notification.setEventType("IMAGE_COPY_STATE");
        notification.setEventTimestamp(new Date());
        notification.setEventMessage(String.valueOf(result.getStatusProgressValue()));
        notification.setOwner(stack.getOwner());
        notification.setAccount(stack.getAccount());
        notification.setCloud(stack.cloudPlatform().toString());
        notification.setRegion(stack.getRegion());
        notification.setStackId(stack.getId());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        return notification;
    }

    private void handleFailure(Stack stack, String errorReason) {
        try {
            if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
            } else {
                // TODO Only trigger the rollback flow
                stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
                connector.rollback(stack, stack.getResources());
                flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, BILLING_STOPPED.name(), errorReason);
            }
        } catch (Exception ex) {
            LOGGER.error("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, String.format("Rollback failed: %s", ex.getMessage()));
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_ROLLBACK_FAILED, CREATE_FAILED.name(), ex.getMessage());
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
        validateResourceResults(cloudContext, res.getErrorDetails(), res.getResults(), true);
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, List<CloudResourceStatus> results, boolean create) {
        String action = create ? "create" : "upscale";
        if (exception != null) {
            LOGGER.error(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext, results.get(0).getStatusReason()));
        }
    }

    private void updateNodeCount(Long stackId, List<Group> originalGroups, List<CloudResourceStatus> statuses, boolean create) {
        for (Group group : originalGroups) {
            int nodeCount = group.getInstances().size();
            List<CloudResourceStatus> failedResources = removeFailedMetadata(stackId, statuses, group);
            if (!failedResources.isEmpty() && create) {
                int failedCount = failedResources.size();
                InstanceGroup instanceGroup = instanceGroupRepository.findOneByGroupNameInStack(stackId, group.getName());
                instanceGroup.setNodeCount(nodeCount - failedCount);
                instanceGroupRepository.save(instanceGroup);
                flowMessageService.fireEventAndLog(stackId, Msg.STACK_INFRASTRUCTURE_ROLLBACK_MESSAGE, Status.UPDATE_IN_PROGRESS.name(),
                        failedCount, group.getName(), failedResources.get(0).getStatusReason());
            }
        }
    }

    private List<CloudResourceStatus> removeFailedMetadata(Long stackId, List<CloudResourceStatus> statuses, Group group) {
        Map<Long, CloudResourceStatus> failedResources = new HashMap<>();
        Set<Long> groupPrivateIds = getPrivateIds(group);
        for (CloudResourceStatus status : statuses) {
            Long privateId = status.getPrivateId();
            if (privateId != null && status.isFailed() && !failedResources.containsKey(privateId) && groupPrivateIds.contains(privateId)) {
                failedResources.put(privateId, status);
                instanceMetadataService.deleteInstanceRequest(stackId, privateId);
            }
        }
        return new ArrayList<>(failedResources.values());
    }

    private Set<Long> getPrivateIds(Group group) {
        Set<Long> ids = new HashSet<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            ids.add(cloudInstance.getTemplate().getPrivateId());
        }
        return ids;
    }
}
