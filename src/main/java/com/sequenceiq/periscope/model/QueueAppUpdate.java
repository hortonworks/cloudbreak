package com.sequenceiq.periscope.model;

import java.util.List;

import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.SchedulerInfo;

public class QueueAppUpdate {

    private final List<ApplicationReport> applicationReports;
    private final SchedulerInfo schedulerInfo;

    public QueueAppUpdate(List<ApplicationReport> applicationReports, SchedulerInfo schedulerInfo) {
        this.applicationReports = applicationReports;
        this.schedulerInfo = schedulerInfo;
    }

    public List<ApplicationReport> getApplicationReports() {
        return applicationReports;
    }

    public SchedulerInfo getSchedulerInfo() {
        return schedulerInfo;
    }
}
