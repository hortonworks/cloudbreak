package com.sequenceiq.flow.reactor.eventbus;

import reactor.bus.Event;

public class EventCanNotBeDeliveredException extends RuntimeException {

    private final Event event;

    public EventCanNotBeDeliveredException(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

}
