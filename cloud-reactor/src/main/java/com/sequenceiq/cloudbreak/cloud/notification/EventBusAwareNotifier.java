package com.sequenceiq.cloudbreak.cloud.notification;

import javax.inject.Inject;

import reactor.bus.EventBus;

public abstract class EventBusAwareNotifier<T> implements CloudbreakNotifier<T> {

    @Inject
    private EventBus eventBus;

    public EventBus getEventBus() {
        return eventBus;
    }
}

