package com.sequenceiq.periscope.monitor.event;

import org.springframework.context.ApplicationEvent;

public class ClusterScaleStatusEvent extends ApplicationEvent {

    public ClusterScaleStatusEvent(long clusterId) {
        super(clusterId);
    }

    public long getClusterId() {
        return (long) source;
    }
}