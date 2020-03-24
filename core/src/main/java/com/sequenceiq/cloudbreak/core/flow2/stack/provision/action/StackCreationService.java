package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackProvisionConstants.START_DATE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_SETUP;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_ROLLBACK_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_TIME;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_METADATA_COLLECTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_PROVISIONED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_PROVISIONING;
import static java.lang.String.format;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.notification.Notification;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

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

    public void setupProvision(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_SETUP, "Provisioning setup");
    }

    public void prepareImage(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.IMAGE_SETUP, "Image setup");
        flowMessageService.fireEventAndLog(stack.getId(), CREATE_IN_PROGRESS.name(), STACK_IMAGE_SETUP);
    }

    public void startProvisioning(StackContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CREATING_INFRASTRUCTURE, "Creating infrastructure");
        flowMessageService.fireEventAndLog(stack.getId(), CREATE_IN_PROGRESS.name(), STACK_PROVISIONING);
        instanceMetaDataService.saveInstanceRequests(stack, context.getCloudStack().getGroups());
    }

    public Stack provisioningFinished(StackContext context, LaunchStackResult result, Map<Object, Object> variables) {
        Date startDate = getStartDateIfExist(variables);
        Stack stack = context.getStack();
        validateResourceResults(context.getCloudContext(), result);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.METADATA_COLLECTION, "Metadata collection");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_TIME,
                String.valueOf(calculateStackCreationTime(startDate)));
        Stack provisionedStack = stackService.getByIdWithListsInTransaction(stack.getId());
        provisionedStack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        return provisionedStack;
    }

    private Date getStartDateIfExist(Map<Object, Object> variables) {
        Date result = null;
        Object startDateObj = variables.get(START_DATE);
        if (startDateObj instanceof Date) {
            result = (Date) startDateObj;
        }
        return result;
    }

    public CheckImageResult checkImage(StackContext context) {
        try {
            Stack stack = context.getStack();
            Image image = imageService.getImage(stack.getId());
            CheckImageRequest<CheckImageResult> checkImageRequest = new CheckImageRequest<>(context.getCloudContext(), context.getCloudCredential(),
                    cloudStackConverter.convert(stack), image);
            LOGGER.debug("Triggering event: {}", checkImageRequest);
            eventBus.notify(checkImageRequest.selector(), eventFactory.createEvent(checkImageRequest));
            CheckImageResult result = checkImageRequest.await();
            sendNotification(result, stack);
            LOGGER.debug("Result: {}", result);
            return result;
        } catch (InterruptedException e) {
            LOGGER.error("Error while executing check image", e);
            throw new OperationException(e);
        } catch (CloudbreakImageNotFoundException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    public Stack setupMetadata(StackContext context, CollectMetadataResult collectMetadataResult) {
        Stack stack = context.getStack();
        metadatSetupService.saveInstanceMetaData(stack, collectMetadataResult.getResults(), InstanceStatus.CREATED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.TLS_SETUP, "TLS setup");
        flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_METADATA_COLLECTED);
        LOGGER.debug("Metadata setup DONE.");
        Stack stackWithMetadata = stackService.getByIdWithListsInTransaction(stack.getId());
        stackWithMetadata.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        return stackWithMetadata;
    }

    public Stack saveTlsInfo(StackContext context, TlsInfo tlsInfo) {
        boolean usePrivateIpToTls = tlsInfo.usePrivateIpToTls();
        Stack stack = context.getStack();
        if (usePrivateIpToTls) {
            SecurityConfig securityConfig = stack.getSecurityConfig();
            securityConfig.setUsePrivateIpToTls(true);
            stackUpdater.updateStackSecurityConfig(stack, securityConfig);
            stack = stackService.getByIdWithListsInTransaction(stack.getId());
            stack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
            LOGGER.debug("Update Stack and it's SecurityConfig to use private ip when TLS is built.");
        }
        return stack;
    }

    public void setupTls(StackContext context) throws CloudbreakException {
        Stack stack = context.getStack();
        for (InstanceMetaData gwInstance : stack.getGatewayInstanceMetadata()) {
            tlsSetupService.setupTls(stack, gwInstance);
        }
    }

    public void stackCreationFinished(Stack stack) {
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_PROVISIONED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISIONED, "Stack provisioned.");
    }

    public void handleStackCreationFailure(StackView stack, Exception errorDetails) {
        LOGGER.info("Error during stack creation flow:", errorDetails);
        String errorReason = errorDetails == null ? "Unknown error" : errorDetails.getMessage();
        if (errorDetails instanceof CancellationException || ExceptionUtils.getRootCause(errorDetails) instanceof CancellationException) {
            LOGGER.debug("The flow has been cancelled.");
        } else {
            if (!stack.isStackInDeletionPhase()) {
                handleFailure(stack, errorReason);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_FAILED, errorReason);
                flowMessageService.fireEventAndLog(stack.getId(), CREATE_FAILED.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            } else {
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            }
        }
    }

    private void sendNotification(CheckImageResult result, Stack stack) {
        notificationSender.send(getImageCopyNotification(result, stack));
    }

    private Notification<CloudbreakEventV4Response> getImageCopyNotification(CheckImageResult result, Stack stack) {
        CloudbreakEventV4Response notification = new CloudbreakEventV4Response();
        notification.setEventType("IMAGE_COPY_STATE");
        notification.setEventTimestamp(new Date().getTime());
        notification.setEventMessage(String.valueOf(result.getStatusProgressValue()));
        notification.setUserId(stack.getCreator().getUserId());
        notification.setWorkspaceId(stack.getWorkspace().getId());
        notification.setCloud(stack.cloudPlatform());
        notification.setRegion(stack.getRegion());
        notification.setStackCrn(stack.getResourceCrn());
        notification.setStackName(stack.getName());
        notification.setStackStatus(stack.getStatus());
        notification.setTenantName(stack.getCreator().getTenant().getName());
        return new Notification<>(notification);
    }

    private void handleFailure(StackView stackView, String errorReason) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackView.getId());
        handleFailure(stack, errorReason);
    }

    private void handleFailure(Stack stack, String errorReason) {
        try {
            if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
            } else {
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ROLLING_BACK);
                connector.rollback(stack, stack.getResources());
                flowMessageService.fireEventAndLog(stack.getId(), CREATE_FAILED.name(), STACK_INFRASTRUCTURE_CREATE_FAILED, errorReason);
            }
        } catch (Exception ex) {
            LOGGER.info("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.PROVISION_FAILED, format("Rollback failed: %s", ex.getMessage()));
            flowMessageService.fireEventAndLog(stack.getId(), CREATE_FAILED.name(), STACK_INFRASTRUCTURE_ROLLBACK_FAILED, ex.getMessage());
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
