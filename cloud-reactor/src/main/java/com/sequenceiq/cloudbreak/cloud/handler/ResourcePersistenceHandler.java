package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationPersisted;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class ResourcePersistenceHandler implements Consumer<Event<ResourceAllocationNotification>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler.class);

    @Inject
    private Persister<ResourceAllocationNotification> cloudResourcePersisterService;

    @Override
    public void accept(Event<ResourceAllocationNotification> resourceAllocationNotificationEvent) {
        LOGGER.info("ResourceAllocationNotification received: {}", resourceAllocationNotificationEvent);
        ResourceAllocationNotification notification = resourceAllocationNotificationEvent.getData();
        notification = cloudResourcePersisterService.persist(notification);
        notification.getPromise().onNext(new ResourceAllocationPersisted(notification));
    }
}
