package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class ResourceDefinitionHandler implements CloudPlatformEventHandler<ResourceDefinitionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDefinitionHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<ResourceDefinitionRequest> type() {
        return ResourceDefinitionRequest.class;
    }

    @Override
    public void accept(Event<ResourceDefinitionRequest> getRegionsRequestEvent) {
        LOGGER.debug("Received event: {}", getRegionsRequestEvent);
        ResourceDefinitionRequest request = getRegionsRequestEvent.getData();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(request.getPlatform());
            String resource = request.getResource();
            String definition = connector.parameters().resourceDefinition(request.getResource());
            if (definition == null) {
                Exception exception = new Exception("Failed to find resource definition for " + resource);
                request.getResult().onNext(new ResourceDefinitionResult(exception.getMessage(), exception, request));
            } else {
                request.getResult().onNext(new ResourceDefinitionResult(request, definition));
            }
        } catch (RuntimeException e) {
            request.getResult().onNext(new ResourceDefinitionResult(e.getMessage(), e, request));
        }
    }
}
