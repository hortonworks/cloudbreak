package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class UpdateFailedEvent extends ApplicationEvent implements UpdateEvent {

    public UpdateFailedEvent(String clusterId) {
        super(clusterId);
    }

    @Override
    public String getClusterId() {
        return (String) source;
    }
}