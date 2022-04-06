package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class ClusterDeleteEvent extends ApplicationEvent {

    public ClusterDeleteEvent(long clusterId) {
        super(clusterId);
    }

    public long getClusterId() {
        return (long) source;
    }
}
