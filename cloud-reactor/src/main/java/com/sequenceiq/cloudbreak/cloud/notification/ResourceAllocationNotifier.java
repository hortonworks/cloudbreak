package com.sequenceiq.cloudbreak.cloud.notification;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationNotification;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationPersisted;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.rx.Promise;
import reactor.rx.Promises;

@Component
public class ResourceAllocationNotifier implements ResourcePersistenceNotifier<ResourceAllocationPersisted> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAllocationNotifier.class);

    @Inject
    private EventBus eventBus;

    @Override
    public Promise<ResourceAllocationPersisted> notifyResourceAllocation(CloudResource cloudResource, StackContext stackContext) {
        LOGGER.info("Assembling resource allocation notification. resource: {}, stack context: {}", cloudResource, stackContext);
        Promise<ResourceAllocationPersisted> promise = Promises.prepare();
        ResourceAllocationNotification notification = new ResourceAllocationNotification(cloudResource, stackContext.getStackId(), promise);
        LOGGER.info("Firing notification: {}", notification);
        eventBus.notify("resource-allocation-persisted", Event.wrap(notification));
        return promise;
    }
}
