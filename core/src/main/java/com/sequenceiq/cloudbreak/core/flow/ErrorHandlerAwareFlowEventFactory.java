package com.sequenceiq.cloudbreak.core.flow;

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
        return new Event(null, payLoad, errorHandler);
    }

}
