package com.sequenceiq.cloudbreak.cloud.handler;

import reactor.bus.Event;
import reactor.fn.Consumer;

public interface CloudPlatformEventHandler<T> extends Consumer<Event<T>> {

    Class<T> type();

}
