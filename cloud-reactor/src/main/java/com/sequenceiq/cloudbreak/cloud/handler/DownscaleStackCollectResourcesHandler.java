package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class DownscaleStackCollectResourcesHandler implements CloudPlatformEventHandler<DownscaleStackCollectResourcesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownscaleStackCollectResourcesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<DownscaleStackCollectResourcesRequest> type() {
        return DownscaleStackCollectResourcesRequest.class;
    }

    @Override
    public void accept(Event<DownscaleStackCollectResourcesRequest> collectResourcesRequestEvent) {
        LOGGER.debug("Received event: {}", collectResourcesRequestEvent);
        DownscaleStackCollectResourcesRequest request = collectResourcesRequestEvent.getData();
        DownscaleStackCollectResourcesResult result;
        try {
            CloudContext cloudContext = request.getCloudContext();
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResource> resourcesToScale = connector.resources().collectResourcesToRemove(ac, request.getCloudStack(),
                    request.getCloudResources(), request.getInstances());
            LOGGER.debug("Collect resources successfully finished for {}", cloudContext);
            result = new DownscaleStackCollectResourcesResult(request.getResourceId(), resourcesToScale);
        } catch (RuntimeException e) {
            LOGGER.info("Failed to handle DownscaleStackCollectResourcesRequest.", e);
            result = new DownscaleStackCollectResourcesResult(e.getMessage(), e, request.getResourceId());
        }
        request.getResult().onNext(result);
        LOGGER.debug("DownscaleStackCollectResourcesRequest finished");
        eventBus.notify(result.selector(), new Event<>(collectResourcesRequestEvent.getHeaders(), result));
    }
}
