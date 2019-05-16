package com.sequenceiq.flow.reactor.api.handler;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.reactor.api.event.EventSender;

public abstract class EventSenderAwareHandler<T extends Payload> implements EventHandler<T> {

    private final EventSender eventSender;

    protected EventSenderAwareHandler(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    protected EventSender eventSender() {
        return eventSender;
    }
}
