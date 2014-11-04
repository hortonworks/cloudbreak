package com.sequenceiq.cloudbreak.service.events;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakEventData>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakEventHandler.class);

    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public void accept(Event<CloudbreakEventData> cloudbreakEvent) {
        CloudbreakEventData event = cloudbreakEvent.getData();
        //LOGGER.info("Cloudbreak event received {}", cloudbreakEvent);
        com.sequenceiq.cloudbreak.domain.CloudbreakEvent registeredEvent =
                eventService.createStackEvent(event.getEntityId(), event.getEventType(), event.getEventMessage());
        //LOGGER.info("Event registered: {}", registeredEvent);
    }
}
