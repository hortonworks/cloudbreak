package com.sequenceiq.cloudbreak.core.flow.handlers;

import reactor.bus.Event;
import reactor.fn.Consumer;

public interface AmbariClusterEventHandler<T> extends Consumer<Event<T>> {

    Class<T> type();

}
