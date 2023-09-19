package com.sequenceiq.flow.reactor.api.handler;

import com.sequenceiq.cloudbreak.eventbus.Event;

public class HandlerEvent<T> {

    private Event<T> event;

    private int eventCounter;

    public HandlerEvent(Event<T> event) {
        this.event = event;
    }

    public Event<T> getEvent() {
        return event;
    }

    public void setEvent(Event<T> event) {
        this.event = event;
    }

    public T getData() {
        return event.getData();
    }

    public void increaseCounter() {
        eventCounter++;
    }

    public int getCounter() {
        return eventCounter;
    }

    @Override
    public String toString() {
        return "HandlerEvent{" +
                "event=" + event +
                ", eventCounter=" + eventCounter +
                '}';
    }
}
