package com.sequenceiq.periscope.monitor.event;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.springframework.context.ApplicationEvent;

public class SchedulerUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final String clusterId;

    public SchedulerUpdateEvent(SchedulerInfo source, String clusterId) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    public SchedulerInfo getSchedulerInfo() {
        return (SchedulerInfo) source;
    }
}
