package com.sequenceiq.flow.reactor.api.handler;

import java.util.function.Consumer;

import com.sequenceiq.cloudbreak.eventbus.Event;

/**
 * @deprecated Please use {@link ExceptionCatcherEventHandler}
 */
@Deprecated
public interface EventHandler<T> extends Consumer<Event<T>> {
    String selector();
}
