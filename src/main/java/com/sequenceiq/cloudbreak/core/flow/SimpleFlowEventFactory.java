package com.sequenceiq.cloudbreak.core.flow;

import org.jvnet.hk2.annotations.Service;

import reactor.event.Event;

@Service
public class SimpleFlowEventFactory implements FlowEventFactory<Object> {
    @Override
    public Event<Object> createEvent(Object payLoad, String eventKey) {
        Event event = new Event(payLoad);
        return event;
    }
}
