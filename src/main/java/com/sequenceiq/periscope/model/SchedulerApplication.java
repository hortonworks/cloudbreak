package com.sequenceiq.periscope.model;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;

public class SchedulerApplication {

    private final ApplicationId applicationId;
    private final long startTime;
    private Priority priority;
    private boolean moved;
    private double progress;

    public SchedulerApplication(ApplicationReport appReport, Priority priority) {
        this.applicationId = appReport.getApplicationId();
        this.startTime = appReport.getStartTime();
        this.priority = priority;
    }

    public ApplicationId getApplicationId() {
        return applicationId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    public long getStartTime() {
        return startTime;
    }

    public double getProgress() {
        return progress;
    }

    public void update(ApplicationReport report) {
        this.progress = report.getProgress();
    }

}
