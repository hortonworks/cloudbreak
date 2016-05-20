package com.sequenceiq.cloudbreak.reactor.handler;

import reactor.bus.Event;
import reactor.fn.Consumer;

public interface ReactorEventHandler<T> extends Consumer<Event<T>> {
    String selector();
}
