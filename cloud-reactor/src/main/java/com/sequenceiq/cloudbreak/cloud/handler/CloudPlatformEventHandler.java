package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.function.Consumer;

import com.sequenceiq.cloudbreak.eventbus.Event;

public interface CloudPlatformEventHandler<T> extends Consumer<Event<T>> {

    Class<T> type();

}
