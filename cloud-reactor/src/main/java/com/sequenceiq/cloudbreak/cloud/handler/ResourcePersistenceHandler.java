package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.retry.RetryUtil;

@Component
public class ResourcePersistenceHandler implements Consumer<Event<ResourceNotification>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler.class);

    @Inject
    private Persister<ResourceNotification> cloudResourcePersisterService;

    @Override
    public void accept(Event<ResourceNotification> event) {
        LOGGER.debug("Resource notification event received: {}", event);
        ResourceNotification notification = event.getData();

        RetryUtil.withDefaultRetries()
                .retry(() -> {
                    ResourceNotification notificationPersisted = switch (notification.getType()) {
                        case CREATE -> cloudResourcePersisterService.persist(notification);
                        case UPDATE -> cloudResourcePersisterService.update(notification);
                        case DELETE -> cloudResourcePersisterService.delete(notification);
                    };
                    notificationPersisted.getPromise().onNext(new ResourcePersisted());
                })
                .checkIfRecoverable(e -> e instanceof TransientDataAccessException)
                .ifNotRecoverable(e -> notification.getPromise().onError(e))
                .run();
    }
}