package com.sequenceiq.cloudbreak.reactor;

import reactor.bus.Event;
import reactor.fn.Consumer;

public interface ClusterEventHandler<T> extends Consumer<Event<T>> {
    Class<T> type();
}
