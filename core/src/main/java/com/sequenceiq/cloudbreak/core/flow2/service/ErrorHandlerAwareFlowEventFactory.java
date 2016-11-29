package com.sequenceiq.cloudbreak.core.flow2.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import reactor.bus.Event;

/**
 * Event factory that registers an error handler into the event.
 */
@Service
public class ErrorHandlerAwareFlowEventFactory implements FlowEventFactory<Object> {

    @Inject
    private CloudbreakErrorHandler errorHandler;

    @Override
    public Event<Object> createEvent(Object payLoad, String eventKey) {
        return createEvent(null, payLoad);
    }

    public Event<Object> createEvent(Event.Headers headers, Object payLoad) {
        return new Event<>(headers, payLoad, errorHandler);
    }

    @Override
    public <P> Event<P> createEvent(P payLoad) {
        return new Event<>(null, payLoad, errorHandler);
    }
}
