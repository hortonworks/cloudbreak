package com.sequenceiq.cloudbreak.cloud.notification;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
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
    public ResourcePersisted notifyAllocation(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.CREATE);
        LOGGER.info("Sending resource allocation notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", Event.wrap(notification));
        return notification.getResult();
    }

    @Override
    public ResourcePersisted notifyUpdate(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.UPDATE);
        LOGGER.info("Sending resource update notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", Event.wrap(notification));
        return notification.getResult();
    }

    @Override
    public ResourcePersisted notifyDeletion(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.DELETE);
        LOGGER.info("Sending resource deletion notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", Event.wrap(notification));
        return notification.getResult();
    }
}
