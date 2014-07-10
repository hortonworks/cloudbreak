package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class QueueInfoUpdateFailedEvent extends ApplicationEvent {

    public QueueInfoUpdateFailedEvent(String clusterId) {
        super(clusterId);
    }

    public String getClusterId() {
        return (String) getSource();
    }
}