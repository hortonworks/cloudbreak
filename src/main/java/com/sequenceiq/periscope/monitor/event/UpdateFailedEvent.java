package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class UpdateFailedEvent extends ApplicationEvent implements UpdateEvent {

    public UpdateFailedEvent(long clusterId) {
        super(clusterId);
    }

    @Override
    public long getClusterId() {
        return (long) source;
    }
}