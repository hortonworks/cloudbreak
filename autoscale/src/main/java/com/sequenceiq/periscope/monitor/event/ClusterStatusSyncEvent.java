package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class ClusterStatusSyncEvent extends ApplicationEvent {

    public ClusterStatusSyncEvent(long clusterId) {
        super(clusterId);
    }

    public long getClusterId() {
        return (long) source;
    }
}