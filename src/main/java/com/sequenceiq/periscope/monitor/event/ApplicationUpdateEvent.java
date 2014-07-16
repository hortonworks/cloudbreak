package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;
import org.springframework.context.ApplicationEvent;

import com.sequenceiq.periscope.monitor.model.QueueAppUpdate;

public class ApplicationUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final String clusterId;

    public ApplicationUpdateEvent(String clusterId, QueueAppUpdate source) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    public List<ApplicationReport> getReports() {
        return ((QueueAppUpdate) source).getApplicationReports();
    }

    public SchedulerInfo getSchedulerInfo() {
        return ((QueueAppUpdate) source).getSchedulerInfo();
    }
}
