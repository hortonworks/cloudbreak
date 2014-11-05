package com.sequenceiq.cloudbreak.service.events;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakEventData>> {

    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public void accept(Event<CloudbreakEventData> cloudbreakEvent) {
        CloudbreakEventData event = cloudbreakEvent.getData();
        com.sequenceiq.cloudbreak.domain.CloudbreakEvent registeredEvent =
                eventService.createStackEvent(event.getEntityId(), event.getEventType(), event.getEventMessage());
    }
}
