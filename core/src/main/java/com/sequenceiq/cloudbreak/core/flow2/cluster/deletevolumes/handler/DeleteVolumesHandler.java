package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DELETE_VOLUMES_FAILED;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteVolumesHandler extends ExceptionCatcherEventHandler<DeleteVolumesHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesHandlerRequest> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesHandlerRequest> deleteVolumesEvent) {
        LOGGER.debug("Staring DeleteVolumesHandler with event: {}", deleteVolumesEvent);
        DeleteVolumesHandlerRequest payload = deleteVolumesEvent.getData();
        StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
        List<CloudResource> cloudResourcesToBeDeleted = payload.getResourcesToBeDeleted();
        String requestGroup = stackDeleteVolumesRequest.getGroup();
        String cloudPlatform = payload.getCloudPlatform();
        Long stackId = stackDeleteVolumesRequest.getStackId();
        try {
            StackDto stack = stackDtoService.getById(payload.getResourceId());
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(stack.getPlatformVariant()));
            CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
            CloudContext cloudContext = getCloudContext(stack);
            AuthenticatedContext ac = getAuthenticatedContext(cloudCredential, cloudContext, cloudConnector);
            LOGGER.debug("Starting detach volumes for resources: {}", cloudResourcesToBeDeleted);
            deleteVolumesService.detachResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
            LOGGER.debug("Starting to delete volumes on cloud provider for stack: {}", stack);
            deleteVolumes(cloudResourcesToBeDeleted, cloudPlatformVariant, ac, stackDeleteVolumesRequest, requestGroup);
            LOGGER.debug("Starting to delete volumes on CBDB for stack: {}", stack);
            deleteVolumesService.deleteVolumeResources(stack, payload);
            deleteVolumesService.updateScriptsAndRebootInstances(stackId, requestGroup);
            return new DeleteVolumesFinishedEvent(stackDeleteVolumesRequest);
        } catch (Exception ex) {
            LOGGER.warn("Detaching and deleting block storage disks failed for stack: {}, and group: {}, Exception:: {}",
                    stackDeleteVolumesRequest.getStackId(), requestGroup, ex.getCause());
            return new DeleteVolumesFailedEvent(ex.getMessage(), ex, stackId);
        }
    }

    private AuthenticatedContext getAuthenticatedContext(CloudCredential cloudCredential, CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, cloudCredential);
    }

    private void deleteVolumes(List<CloudResource> cloudResourcesToBeDeleted, CloudPlatformVariant cloudPlatformVariant, AuthenticatedContext ac,
            StackDeleteVolumesRequest stackDeleteVolumesRequest, String requestGroup) {
        try {
            LOGGER.debug("Deleting attached block storage volumes from CB: {}", cloudResourcesToBeDeleted);
            deleteVolumesService.deleteResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
        } catch (Exception ex) {
            LOGGER.warn("Deleting block storage disks failed for stack: {}, and group: {}.",
                    stackDeleteVolumesRequest.getStackId(), requestGroup);
            LOGGER.warn("Exception while deleting block storages - ", ex);
            flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(),
                    DELETE_FAILED.name(),
                    DELETE_VOLUMES_FAILED,
                    requestGroup);
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }

    private CloudContext getCloudContext(StackDto stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .build();
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesHandlerRequest.class);
    }
}
