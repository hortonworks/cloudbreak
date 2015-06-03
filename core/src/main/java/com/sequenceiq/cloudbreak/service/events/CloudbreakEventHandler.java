package com.sequenceiq.cloudbreak.service.events;


import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class CloudbreakEventHandler implements Consumer<Event<CloudbreakEventData>> {

    @Inject
    private CloudbreakEventService eventService;

    @Override
    public void accept(Event<CloudbreakEventData> cloudbreakEvent) {
        CloudbreakEventData event = cloudbreakEvent.getData();
        MDCBuilder.buildMdcContext(event);
        eventService.createStackEvent(event);
    }
}
