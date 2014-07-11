package com.sequenceiq.periscope.monitor.event;

import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.springframework.context.ApplicationEvent;

public class ApplicationReportUpdateEvent extends ApplicationEvent implements UpdateEvent {

    private final String clusterId;

    public ApplicationReportUpdateEvent(String clusterId, List<ApplicationReport> source) {
        super(source);
        this.clusterId = clusterId;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    public List<ApplicationReport> getReports() {
        return (List<ApplicationReport>) source;
    }
}
