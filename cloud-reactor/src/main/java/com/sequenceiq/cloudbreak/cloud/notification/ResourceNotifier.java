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
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ResourceNotifier implements PersistenceNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNotifier.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public ResourcePersisted notifyAllocation(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.CREATE);
        LOGGER.debug("Sending resource allocation notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", eventFactory.createEvent(notification));
        return notification.getResult();
    }

    @Override
    public ResourcePersisted notifyUpdate(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.UPDATE);
        LOGGER.debug("Sending resource update notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", eventFactory.createEvent(notification));
        return notification.getResult();
    }

    @Override
    public ResourcePersisted notifyDeletion(CloudResource cloudResource, CloudContext cloudContext) {
        ResourceNotification notification = new ResourceNotification(cloudResource, cloudContext, ResourceNotificationType.DELETE);
        LOGGER.debug("Sending resource deletion notification: {}, context: {}", notification, cloudContext);
        eventBus.notify("resource-persisted", eventFactory.createEvent(notification));
        return notification.getResult();
    }
}
