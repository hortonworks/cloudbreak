package com.sequenceiq.environment.environment.flow.creation.handler.computecluster;

public class ComputeClusterPollerObject {

    private final Long environmentId;

    private final String environmentCrn;

    private final String name;

    private boolean creationInProgressNotificationSent;

    public ComputeClusterPollerObject(Long environmentId, String environmentCrn, String name) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
        this.name = name;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getName() {
        return name;
    }

    public boolean isCreationInProgressNotificationSent() {
        return creationInProgressNotificationSent;
    }

    public void creationInProgressNotificationSent() {
        creationInProgressNotificationSent = true;
    }
}
