package com.sequenceiq.flow.reactor.api.handler;

import reactor.bus.Event;
import reactor.fn.Consumer;

/**
 * @deprecated Please use ExceptionCatcherEventHandler
 */
@Deprecated
public interface EventHandler<T> extends Consumer<Event<T>> {
    String selector();
}
