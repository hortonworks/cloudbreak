package com.sequenceiq.cloudbreak.core.flow2;

import reactor.bus.Event;

public interface Chainable {
    String nextSelector();
    Object nextPayload(Event<?> event);
}
