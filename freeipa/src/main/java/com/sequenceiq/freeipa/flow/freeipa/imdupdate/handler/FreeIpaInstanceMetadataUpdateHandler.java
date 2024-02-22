package com.sequenceiq.freeipa.flow.freeipa.imdupdate.handler;

import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_FAILURE_EVENT;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateRequest;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateResult;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component
public class FreeIpaInstanceMetadataUpdateHandler implements CloudPlatformEventHandler<FreeIpaInstanceMetadataUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceMetadataUpdateHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public Class<FreeIpaInstanceMetadataUpdateRequest> type() {
        return FreeIpaInstanceMetadataUpdateRequest.class;
    }

    @Override
    public void accept(Event<FreeIpaInstanceMetadataUpdateRequest> event) {
        FreeIpaInstanceMetadataUpdateRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            List<Resource> resources = resourceService.findAllByStackId(request.getResourceId());
            List<CloudResource> cloudResources = resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            connector.resources().update(ac, request.getCloudStack(), cloudResources, getUpdateType(request), Optional.empty());
            stackUpdater.updateSupportedImdsVersion(request.getResourceId(), request.getUpdateType());
            FreeIpaInstanceMetadataUpdateResult result = new FreeIpaInstanceMetadataUpdateResult(request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.error("Instance metadata update failed: ", e);
            FreeIpaInstanceMetadataUpdateFailureEvent result = new FreeIpaInstanceMetadataUpdateFailureEvent(request.getResourceId(), e);
            eventBus.notify(STACK_IMDUPDATE_FAILURE_EVENT.event(), new Event<>(event.getHeaders(), result));
        }
    }

    private UpdateType getUpdateType(FreeIpaInstanceMetadataUpdateRequest request) {
        return switch (request.getUpdateType()) {
            case IMDS_HTTP_TOKEN_REQUIRED -> UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED;
            default -> UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_OPTIONAL;
        };
    }

}
