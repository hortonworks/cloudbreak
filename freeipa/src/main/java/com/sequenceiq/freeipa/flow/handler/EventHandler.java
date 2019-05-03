package com.sequenceiq.freeipa.flow.handler;

import reactor.bus.Event;
import reactor.fn.Consumer;

public interface EventHandler<T> extends Consumer<Event<T>> {
    String selector();
}
