package com.sequenceiq.environment.credential.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Service
public class ResourceDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDefinitionService.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Cacheable("resourceDefinitionCache")
    public String getResourceDefinition(String cloudPlatform, String resource) {
        LOGGER.debug("Sending request for {} {} resource property definition", cloudPlatform, resource);
        CloudPlatformVariant platformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.EMPTY);
        ResourceDefinitionRequest request = new ResourceDefinitionRequest(platformVariant, resource);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            ResourceDefinitionResult result = request.await();
            LOGGER.debug("Resource property definition: {}", result);
            return result.getDefinition();
        } catch (InterruptedException e) {
            LOGGER.info("Error while sending resource definition request", e);
            throw new OperationException(e);
        }
    }
}
