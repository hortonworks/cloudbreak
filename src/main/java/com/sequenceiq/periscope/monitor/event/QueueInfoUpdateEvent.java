package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.springframework.context.ApplicationEvent;

public class QueueInfoUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final String clusterId;

    public QueueInfoUpdateEvent(String clusterId, List<QueueInfo> source) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    public List<QueueInfo> getQueueInfo() {
        return (List<QueueInfo>) source;
    }
}
