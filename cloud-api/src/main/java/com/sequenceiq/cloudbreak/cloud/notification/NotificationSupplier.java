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
public class NotificationSupplier implements ResourcePersistenceNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSupplier.class);

    @Inject
    private EventBus eventBus;


    @Override
    public Promise<ResourceAllocationPersisted> notifyResourceAllocation(CloudResource cloudResource, StackContext stackContext) {
        Promise<ResourceAllocationPersisted> promise = Promises.prepare();
        ResourceAllocationNotification notification = new ResourceAllocationNotification(cloudResource, stackContext.getStackId(), promise);
        eventBus.notify("ResourceAllocationPersisted", Event.wrap(notification));
        return promise;
    }
}
