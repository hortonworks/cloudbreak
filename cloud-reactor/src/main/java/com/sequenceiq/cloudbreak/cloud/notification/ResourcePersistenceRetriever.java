package com.sequenceiq.cloudbreak.cloud.notification;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceRetrievalNotification;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ResourcePersistenceRetriever implements PersistenceRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceRetriever.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public Optional<CloudResource> notifyRetrieve(String resourceReference, CommonStatus status, ResourceType resourceType) {
        ResourceRetrievalNotification notification = new ResourceRetrievalNotification(resourceReference, status, resourceType);
        LOGGER.debug("Sending notification to retrieve resources by resource reference: {}", resourceReference);
        eventBus.notify("resource-retrieved", eventFactory.createEvent(notification));
        return notification.getResult();
    }

    @Override
    public Optional<CloudResource> notifyRetrieve(Long stackId, String resourceReference, CommonStatus status, ResourceType resourceType) {
        ResourceRetrievalNotification notification = new ResourceRetrievalNotification(resourceReference, status, resourceType, stackId);
        LOGGER.debug("Sending notification to retrieve resources by resource reference: {} and stack: {}", resourceReference, stackId);
        eventBus.notify("resource-retrieved", eventFactory.createEvent(notification));
        return notification.getResult();
    }
}
