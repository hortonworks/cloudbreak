package com.sequenceiq.cloudbreak.core.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.event.Event;

/**
 * Even factory that registers an error handler into the event.
 */
@Service
public class ErrorHandlerAwareFlowEventFactory implements FlowEventFactory<Object> {

    @Autowired
    private CloudbreakErrorHandler errorHandler;

    @Override
    public Event<Object> createEvent(Object payLoad, String eventKey) {
        return new Event(null, payLoad, errorHandler);
    }

}
