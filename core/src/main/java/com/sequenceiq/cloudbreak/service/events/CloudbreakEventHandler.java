package com.sequenceiq.cloudbreak.service.events;


import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakEventData>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakEventHandler.class);

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private NotificationAssemblingService notificationAssemblingService;

    @Override
    public void accept(Event<CloudbreakEventData> cloudbreakEvent) {
        LOGGER.info("Handling cloudbreak event: {}", cloudbreakEvent);
        CloudbreakEventData event = cloudbreakEvent.getData();
        MDCBuilder.buildMdcContext(event);
        LOGGER.info("Persisting data: {}", event);
        CloudbreakEvent persistedEvent = eventService.createStackEvent(event);
        LOGGER.info("Sending notification with data: {}", persistedEvent);
        notificationSender.send(notificationAssemblingService.createNotification(persistedEvent));
    }
}
