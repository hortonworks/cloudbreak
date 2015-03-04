package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.model.QueueAppUpdate;

public class ApplicationUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final long clusterId;

    public ApplicationUpdateEvent(long clusterId, QueueAppUpdate source) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public long getClusterId() {
        return clusterId;
    }

    @Override
    public EventType getEventType() {
        return EventType.APP_METRIC;
    }

    public List<ApplicationReport> getReports() {
        return ((QueueAppUpdate) source).getApplicationReports();
    }

    public SchedulerInfo getSchedulerInfo() {
        return ((QueueAppUpdate) source).getSchedulerInfo();
    }
}
