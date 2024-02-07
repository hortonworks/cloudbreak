package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;
import static com.sequenceiq.redbeams.api.model.common.Status.AVAILABLE;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateResult;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class StackInstanceMetadataUpdateHandler implements CloudPlatformEventHandler<StackInstanceMetadataUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInstanceMetadataUpdateHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private StackDtoService stackService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public Class<StackInstanceMetadataUpdateRequest> type() {
        return StackInstanceMetadataUpdateRequest.class;
    }

    @Override
    public void accept(Event<StackInstanceMetadataUpdateRequest> event) {
        StackInstanceMetadataUpdateRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            StackDto stack = stackService.getById(request.getResourceId());
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResource> cloudResources = stack.getResources().stream().map(resource ->
                    cloudResourceConverter.convert(resource)).collect(Collectors.toList());
            cloudbreakEventService.fireCloudbreakEvent(request.getResourceId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.IMD_UPDATE_STARTED,
                    List.of(getUpdateTypeMessage(request.getUpdateType())));
            clusterService.updateClusterStatusByStackId(request.getResourceId(), DetailedStackStatus.IMD_UPDATE_STARTED);
            connector.resources().update(ac, request.getCloudStack(), cloudResources, getUpdateType(request), Optional.empty());
            stackUpdater.updateSupportedImdsVersionIfNecessary(stack.getId(), request.getUpdateType());
            cloudbreakEventService.fireCloudbreakEvent(request.getResourceId(), AVAILABLE.name(), ResourceEvent.IMD_UPDATE_FINISHED);
            clusterService.updateClusterStatusByStackId(request.getResourceId(), DetailedStackStatus.IMD_UPDATE_FINISHED);
            StackInstanceMetadataUpdateResult result = new StackInstanceMetadataUpdateResult(request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.error("Instance metadata update failed: ", e);
            cloudbreakEventService.fireCloudbreakEvent(request.getResourceId(), AVAILABLE.name(), ResourceEvent.IMD_UPDATE_FAILED, List.of(e.getMessage()));
            clusterService.updateClusterStatusByStackId(request.getResourceId(), DetailedStackStatus.IMD_UPDATE_FAILED);
            StackInstanceMetadataUpdateFailureEvent result = new StackInstanceMetadataUpdateFailureEvent(request.getResourceId(), e);
            eventBus.notify(STACK_IMDUPDATE_FAILURE_EVENT.event(), new Event<>(event.getHeaders(), result));
        }
    }

    public String getUpdateTypeMessage(InstanceMetadataUpdateType updateType) {
        String code = updateType.getClass().getSimpleName() + "." + updateType.name();
        try {
            return cloudbreakMessagesService.getMessage(code);
        } catch (Exception e) {
            LOGGER.error("Failed to get message for property: {}", code, e);
            return updateType.name();
        }
    }

    private UpdateType getUpdateType(StackInstanceMetadataUpdateRequest request) {
        return switch (request.getUpdateType()) {
            case IMDS_HTTP_TOKEN_REQUIRED -> UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED;
            default -> UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL;
        };
    }

}
