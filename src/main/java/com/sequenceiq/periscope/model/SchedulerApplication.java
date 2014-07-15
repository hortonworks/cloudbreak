package com.sequenceiq.periscope.model;

import org.apache.hadoop.yarn.api.records.ApplicationId;

public class SchedulerApplication {

    private final ApplicationId applicationId;

    public SchedulerApplication(ApplicationId applicationId) {
        this.applicationId = applicationId;
    }

    public ApplicationId getApplicationId() {
        return applicationId;
    }
}
