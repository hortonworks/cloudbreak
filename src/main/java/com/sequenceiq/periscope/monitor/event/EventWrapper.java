package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class EventWrapper<T extends TypedEvent> extends ApplicationEvent {

    public EventWrapper(T event) {
        super(event);
    }

    @Override
    public TypedEvent getSource() {
        return (TypedEvent) super.getSource();
    }
}
