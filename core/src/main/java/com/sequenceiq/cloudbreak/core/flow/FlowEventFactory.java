package com.sequenceiq.cloudbreak.core.flow;

import reactor.bus.Event;

public interface FlowEventFactory<T> {
    Event<T> createEvent(T payLoad, String eventKey);
}
