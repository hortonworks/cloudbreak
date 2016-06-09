package com.sequenceiq.cloudbreak.core.flow2.service;

import reactor.bus.Event;

public interface FlowEventFactory<T> {
    Event<T> createEvent(T payLoad, String eventKey);
}
