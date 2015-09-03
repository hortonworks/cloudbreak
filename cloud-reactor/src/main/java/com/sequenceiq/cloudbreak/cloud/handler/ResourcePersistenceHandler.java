package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class ResourcePersistenceHandler implements Consumer<Event<ResourceNotification>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler.class);

    @Inject
    private Persister<ResourceNotification> cloudResourcePersisterService;

    @Override
    public void accept(Event<ResourceNotification> event) {
        LOGGER.info("Resource notification event received: {}", event);
        ResourceNotification notification = event.getData();
        notification = notification.isCreate() ? cloudResourcePersisterService.persist(notification)
                : cloudResourcePersisterService.delete(notification);
        notification.getPromise().onNext(new ResourcePersisted(notification));
    }
}
