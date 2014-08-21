package com.sequenceiq.periscope.monitor.event;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.context.ApplicationEvent;

public class ClusterMetricsUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final long clusterId;

    public ClusterMetricsUpdateEvent(ClusterMetricsInfo source, long clusterId) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

    public ClusterMetricsInfo getClusterMetricsInfo() {
        return (ClusterMetricsInfo) source;
    }
}
