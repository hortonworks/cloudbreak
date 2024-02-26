package com.sequenceiq.flow.reactor.api.handler;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.reactor.api.event.EventSender;

public abstract class ExceptionCatcherEventSenderAwareHandler<T extends Payload> extends ExceptionCatcherEventHandler<T> {

    private final EventSender eventSender;

    protected ExceptionCatcherEventSenderAwareHandler(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    protected EventSender eventSender() {
        return eventSender;
    }
}
