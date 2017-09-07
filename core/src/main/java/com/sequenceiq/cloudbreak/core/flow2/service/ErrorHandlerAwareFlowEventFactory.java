package com.sequenceiq.cloudbreak.core.flow2.service;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

/**
 * Event factory that registers an error handler into the event.
 */
@Service
public class ErrorHandlerAwareFlowEventFactory {

    @Inject
    private CloudbreakErrorHandler errorHandler;

    public <P> Event<P> createEventWithErrHandler(P payLoad) {
        return createEventWithErrHandler(Maps.newHashMap(), payLoad);
    }

    public <P> Event<P> createEventWithErrHandler(Map<String, Object> headers, P payLoad) {
        return new Event<>(new Headers(headers), payLoad, errorHandler);
    }

    public <P> Event<P> createEvent(P payLoad) {
        return createEvent(Maps.newHashMap(), payLoad);
    }

    public <P> Event<P> createEvent(Map<String, Object> headers, P payLoad) {
        return new Event<>(new Headers(headers), payLoad);
    }
}
