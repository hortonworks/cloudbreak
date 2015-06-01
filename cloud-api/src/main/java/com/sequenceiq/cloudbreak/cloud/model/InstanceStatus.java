package com.sequenceiq.cloudbreak.cloud.model;

public enum InstanceStatus {

    CREATED(StatusGroup.PERMANENT),
    STARTED(StatusGroup.PERMANENT),
    STOPPED(StatusGroup.PERMANENT),
    TERMINATED(StatusGroup.PERMANENT),
    IN_PROGRESS(StatusGroup.TRANSIENT);

    private StatusGroup statusGroup;

    private InstanceStatus(StatusGroup statusGroup) {
        this.statusGroup = statusGroup;
    }

    public StatusGroup getStatusGroup() {
        return statusGroup;
    }

    public boolean isPermanent() {
        return StatusGroup.PERMANENT == statusGroup;
    }

    public boolean isTransient() {
        return StatusGroup.TRANSIENT == statusGroup;
    }
}
