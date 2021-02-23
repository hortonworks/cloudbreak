package com.sequenceiq.flow.reactor.handler;

public class ConsumerNotFoundException extends RuntimeException {

    private final Object event;

    public ConsumerNotFoundException(Object event) {
        this.event = event;
    }

    public Object getEvent() {
        return event;
    }

}
