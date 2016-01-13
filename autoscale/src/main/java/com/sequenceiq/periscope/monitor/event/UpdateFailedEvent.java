package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class UpdateFailedEvent extends ApplicationEvent {

    public UpdateFailedEvent(long clusterId) {
        super(clusterId);
    }

    public long getClusterId() {
        return (long) source;
    }

}