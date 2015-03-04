package com.sequenceiq.periscope.monitor.event;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.context.ApplicationEvent;

public class YarnMetricUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final long clusterId;

    public YarnMetricUpdateEvent(ClusterMetricsInfo source, long clusterId) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

    @Override
    public EventType getEventType() {
        return EventType.YARN_METRIC;
    }

    public ClusterMetricsInfo getClusterMetricsInfo() {
        return (ClusterMetricsInfo) source;
    }
}
