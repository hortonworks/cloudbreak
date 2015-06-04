package com.sequenceiq.cloudbreak.service.eventbus;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationPersisted;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class ResourcePersistenceHandler implements Consumer<Event<ResourceAllocationNotification>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler.class);
    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private StackService stackService;

    @Override
    public void accept(Event<ResourceAllocationNotification> resourceAllocationNotificationEvent) {
        LOGGER.info("ResourceAllocationNotification received: {}", resourceAllocationNotificationEvent);

        ResourceAllocationNotification notification = resourceAllocationNotificationEvent.getData();
        CloudResource cloudResource = notification.getCloudResource();

        Resource resource = transform(cloudResource);
        resource.setStack(stackService.getById(notification.getStackId()));
        resource = resourceRepository.save(resource);

        LOGGER.info("Allocated resource saved: {}", resource);
        notification.getPromise().onNext(new ResourceAllocationPersisted(notification));
    }

    private Resource transform(CloudResource cloudResource) {
        return new Resource(cloudResource.getType(), cloudResource.getReference(), null);
    }
}
