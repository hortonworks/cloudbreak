package com.sequenceiq.cloudbreak.core.flow;

import reactor.event.Event;

public interface FlowEventFactory<T> {
    Event<T> createEvent(T payLoad, String eventKey);
}
