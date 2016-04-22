package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

import reactor.bus.Event;

public interface Chainable {
    String nextSelector();
    Object nextPayload(Event<? extends Payload> event);
}
