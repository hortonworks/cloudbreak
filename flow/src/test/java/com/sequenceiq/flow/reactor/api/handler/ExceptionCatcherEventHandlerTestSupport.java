package com.sequenceiq.flow.reactor.api.handler;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.bus.Event;

/**
 * Support class to make unit testing of {@link ExceptionCatcherEventHandler} easier.
 * @param <T> request event payload type
 */
public class ExceptionCatcherEventHandlerTestSupport<T extends Payload> {

    private final ExceptionCatcherEventHandler<T> eventHandler;

    /**
     * Creates a new {@code ExceptionCatcherEventHandlerTestSupport} instance wrapping {@code eventHandler}.
     * @param eventHandler {@link ExceptionCatcherEventHandler} instance to wrap; must not be {@code null}
     * @throws NullPointerException if {@code eventHandler == null}
     */
    public ExceptionCatcherEventHandlerTestSupport(ExceptionCatcherEventHandler<T> eventHandler) {
        this.eventHandler = Objects.requireNonNull(eventHandler);
    }

    /**
     * Delegates to {@link ExceptionCatcherEventHandler#doAccept(ExceptionCatcherEventHandler.HandlerEvent)}.
     * @param event reactor event
     * @return response event payload
     */
    public Selectable doAccept(Event<T> event) {
        return eventHandler.doAccept(eventHandler.new HandlerEvent(event));
    }

}
