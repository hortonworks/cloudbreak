package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.VOLUMES_DELETE_FAILED;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
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

    @Inject
    private EventBus eventBus;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesHandlerRequest> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesHandlerRequest> deleteVolumesEvent) {
        LOGGER.debug("Staring DeleteVolumesHandler with event: {}", deleteVolumesEvent);
        DeleteVolumesHandlerRequest payload = deleteVolumesEvent.getData();
        StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
        StackDto stack = stackDtoService.getById(payload.getResourceId());
        String requestGroup = stackDeleteVolumesRequest.getGroup();
        String cloudPlatform = payload.getCloudPlatform();
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(stack.getPlatformVariant()));
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .build();
        AuthenticatedContext ac = getAuthenticatedContext(cloudCredential, cloudContext, cloudConnector);
        List<CloudResource> cloudResourcesToBeDeleted = payload.getResourcesToBeDeleted();
        Set<ServiceComponent> hostTemplateServiceComponents = payload.getHostTemplateServiceComponents();
        try {
            LOGGER.debug("Staring detach volumes for resources: {}", cloudResourcesToBeDeleted);
            deleteVolumesService.detachResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
            LOGGER.debug("Staring to delete volumes on cloud provider for stack: {}", stack);
            deleteVolumesService.deleteVolumeResources(stack, payload);
            LOGGER.debug("Staring CM services for stack: {}", stack);
            deleteVolumesService.startClouderaManagerService(stack, hostTemplateServiceComponents);
            deleteVolumes(cloudResourcesToBeDeleted, cloudPlatformVariant, ac, stackDeleteVolumesRequest, requestGroup);
            return new DeleteVolumesFinishedEvent(stackDeleteVolumesRequest);
        } catch (Exception ex) {
            LOGGER.error("Detaching EBS disks failed for stack: {}, and group: {}, Exception:: {}", stackDeleteVolumesRequest.getStackId(),
                    requestGroup, ex.getCause());
            return new DeleteVolumesFailedEvent(ex.getMessage(), ex, stack.getId());
        }
    }

    private AuthenticatedContext getAuthenticatedContext(CloudCredential cloudCredential, CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, cloudCredential);
    }

    private void deleteVolumes(List<CloudResource> cloudResourcesToBeDeleted, CloudPlatformVariant cloudPlatformVariant, AuthenticatedContext ac,
            StackDeleteVolumesRequest stackDeleteVolumesRequest, String requestGroup) {
        try {
            LOGGER.debug("Deleting attached EBS volumes from CB: {}", cloudResourcesToBeDeleted);
            deleteVolumesService.deleteResources(cloudResourcesToBeDeleted, cloudPlatformVariant, ac);
        } catch (Exception ex) {
            List<String> volumeIds = cloudResourcesToBeDeleted.stream().filter(res -> null != res.getInstanceId())
                    .map(resource -> resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes())
                    .flatMap(Collection::stream)
                    .map(VolumeSetAttributes.Volume::getId)
                    .collect(toList());
            LOGGER.error("Deleting EBS disks failed for stack: {}, and group: {}. Please delete them manually, volume IDs: {}",
                    stackDeleteVolumesRequest.getStackId(), requestGroup, volumeIds);
            flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(),
                    DELETE_FAILED.name(),
                    VOLUMES_DELETE_FAILED,
                    requestGroup,
                    String.join(",", volumeIds));
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesHandlerRequest.class);
    }
}
