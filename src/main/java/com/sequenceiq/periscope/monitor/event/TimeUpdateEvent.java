package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class TimeUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final long clusterId;

    public TimeUpdateEvent(long clusterId) {
        super(new Object());
        this.clusterId = clusterId;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

    @Override
    public EventType getEventType() {
        return EventType.TIME;
    }

}
