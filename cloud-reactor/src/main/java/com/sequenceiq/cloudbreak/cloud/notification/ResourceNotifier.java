package com.sequenceiq.cloudbreak.cloud.notification;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudNotificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotificationType;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ResourceNotifier implements PersistenceNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNotifier.class);

    @Inject
    private EventBus eventBus;

    @Override
    public ResourcePersisted notifyAllocation(CloudResource cloudResource, CloudContext cloudContext) throws CloudNotificationException {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.CREATE);
        return sendNotification(notification, cloudContext);
    }

    @Override
    public ResourcePersisted notifyUpdate(CloudResource cloudResource, CloudContext cloudContext) throws CloudNotificationException {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.UPDATE);
        return sendNotification(notification, cloudContext);
    }

    @Override
    public ResourcePersisted notifyDeletion(CloudResource cloudResource, CloudContext cloudContext) throws CloudNotificationException {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.DELETE);
        return sendNotification(notification, cloudContext);
    }

    private ResourcePersisted sendNotification(ResourceNotification notification, CloudContext cloudContext) throws CloudNotificationException {
        LOGGER.info("Sending resource {} notification: {}, context: {}", notification.getType(), notification, cloudContext);
        synchronized (notification) {
            try {
                eventBus.notify("resource-persisted", Event.wrap(notification));
                notification.wait();
            } catch (InterruptedException e) {
                throw new CloudNotificationException(e.getMessage());
            }
        }
        if (notification.isFailed()) {
            throw new CloudNotificationException(notification.getError());
        }
        return notification.getResource();
    }
}
