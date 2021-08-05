package com.sequenceiq.flow.reactor;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

/**
 * Event factory that registers an error handler into the event.
 */
@Service
public class ErrorHandlerAwareReactorEventFactory {

    @Inject
    private LoggingErrorHandler errorHandler;

    public <P> Event<P> createEventWithErrHandler(P payLoad) {
        return createEventWithErrHandler(Maps.newHashMap(), payLoad);
    }

    public <P> Event<P> createEventWithErrHandler(Map<String, Object> headers, P payLoad) {
        Map<String, Object> extendedHeaders = new HashMap<>(headers);
        extendedHeaders.put(MDCBuilder.MDC_CONTEXT_ID, MDCBuilder.getMdcContextMap());
        return new Event<>(new Headers(extendedHeaders), payLoad, errorHandler);
    }

    public <P> Event<P> createEvent(P payLoad) {
        return createEvent(Maps.newHashMap(), payLoad);
    }

    public <P> Event<P> createEvent(Map<String, Object> headers, P payLoad) {
        Map<String, Object> extendedHeaders = new HashMap<>(headers);
        extendedHeaders.put(MDCBuilder.MDC_CONTEXT_ID, MDCBuilder.getMdcContextMap());
        return new Event<>(new Headers(extendedHeaders), payLoad);
    }
}
