package com.sequenceiq.periscope.monitor.event;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.context.ApplicationEvent;

public class ClusterMetricsUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final String clusterId;

    public ClusterMetricsUpdateEvent(ClusterMetricsInfo source, String clusterId) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    public ClusterMetricsInfo getClusterMetricsInfo() {
        return (ClusterMetricsInfo) source;
    }
}
