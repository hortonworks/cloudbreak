package com.sequenceiq.cloudbreak.service.events;


import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.structuredevent.StructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<StructuredNotificationEvent>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakEventHandler.class);

    @Inject
    private StructuredEventClient structuredEventClient;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private NotificationAssemblingService<Object> notificationAssemblingService;

    @Inject
    private ConversionService conversionService;

    @Override
    public void accept(Event<StructuredNotificationEvent> cloudbreakEvent) {
        StructuredNotificationEvent structuredNotificationEvent = cloudbreakEvent.getData();
        structuredEventClient.sendStructuredEvent(structuredNotificationEvent);
        CloudbreakEventV4Response cloudbreakEventV4Response = conversionService.convert(structuredNotificationEvent, CloudbreakEventV4Response.class);
        notificationSender.send(notificationAssemblingService.createNotification(cloudbreakEventV4Response));
    }
}
