package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.retry.ErrorTask;
import com.sequenceiq.cloudbreak.cloud.retry.ExceptionCheckTask;
import com.sequenceiq.cloudbreak.cloud.retry.RetryTask;
import com.sequenceiq.cloudbreak.cloud.retry.RetryUtil;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class ResourcePersistenceHandler implements Consumer<Event<ResourceNotification>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler.class);

    @Inject
    private Persister<ResourceNotification> cloudResourcePersisterService;

    @Override
    public void accept(final Event<ResourceNotification> event) {
        LOGGER.info("Resource notification event received: {}", event);
        final ResourceNotification notification = event.getData();

        RetryUtil.withDefaultRetries()
                .retry(new RetryTask() {
                    @Override
                    public void run() throws Exception {
                        ResourceNotification notificationPersisted;
                        switch (notification.getType()) {
                            case CREATE:
                                notificationPersisted = cloudResourcePersisterService.persist(notification);
                                break;
                            case UPDATE:
                                notificationPersisted = cloudResourcePersisterService.update(notification);
                                break;
                            case DELETE:
                                notificationPersisted = cloudResourcePersisterService.delete(notification);
                                break;
                            default:
                                awaken(notification);
                                throw new IllegalArgumentException("Unsupported notification type: " + notification.getType());
                        }
                        notificationPersisted.setResource(new ResourcePersisted(notificationPersisted));
                        awaken(notification);
                    }
                })
                .checkIfRecoverable(new ExceptionCheckTask() {
                    @Override
                    public boolean check(Exception e) {
                        return e instanceof TransientDataAccessException;
                    }
                })
                .ifNotRecoverable(new ErrorTask() {
                    @Override
                    public void run(Exception e) {
                        LOGGER.error("Resource persistence operation went fail.", e);
                        notification.setError(e.getMessage() != null ? e.getMessage() : "Unknown reason");
                        awaken(notification);
                    }
                }).run();
    }

    private void awaken(ResourceNotification notification) {
        synchronized (notification) {
            notification.notifyAll();
        }
    }
}
