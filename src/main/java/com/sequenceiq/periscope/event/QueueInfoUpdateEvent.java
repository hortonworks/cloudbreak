package com.sequenceiq.periscope.event;

import java.util.List;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.springframework.context.ApplicationEvent;

public class QueueInfoUpdateEvent extends ApplicationEvent {

    private final String clusterId;

    public QueueInfoUpdateEvent(String clusterId, List<QueueInfo> source) {
        super(source);
        this.clusterId = clusterId;
    }

    public String getClusterId() {
        return clusterId;
    }
}
