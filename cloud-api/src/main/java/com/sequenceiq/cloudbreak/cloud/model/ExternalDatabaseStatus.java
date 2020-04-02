package com.sequenceiq.cloudbreak.cloud.model;

public enum ExternalDatabaseStatus {
    STARTED(StatusGroup.PERMANENT),
    STOPPED(StatusGroup.PERMANENT),
    UPDATE_IN_PROGRESS(StatusGroup.TRANSIENT);

    private final StatusGroup statusGroup;

    ExternalDatabaseStatus(StatusGroup statusGroup) {
        this.statusGroup = statusGroup;
    }

    public boolean isPermanent() {
        return StatusGroup.PERMANENT == statusGroup;
    }

    public boolean isTransient() {
        return StatusGroup.TRANSIENT == statusGroup;
    }

    public StatusGroup getStatusGroup() {
        return statusGroup;
    }
}
