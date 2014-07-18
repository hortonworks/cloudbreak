package com.sequenceiq.periscope.model;

import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.util.ConverterUtils;

public class SchedulerApplication {

    private final ApplicationId applicationId;
    private Priority priority;
    private boolean moved;

    public SchedulerApplication(String applicationId) {
        this(applicationId, Priority.NORMAL);
    }

    public SchedulerApplication(String applicationId, Priority priority) {
        this(ConverterUtils.toApplicationId(applicationId), priority);
    }

    public SchedulerApplication(ApplicationId applicationId, Priority priority) {
        this.applicationId = applicationId;
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
}
