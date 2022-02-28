package com.sequenceiq.cloudbreak.cloud.model;

public enum InstanceStatus {

    CREATED(StatusGroup.PERMANENT),
    STARTED(StatusGroup.PERMANENT),
    STOPPED(StatusGroup.PERMANENT),
    FAILED(StatusGroup.PERMANENT),
    TERMINATED(StatusGroup.PERMANENT),
    TERMINATED_BY_PROVIDER(StatusGroup.PERMANENT),
    UNKNOWN(StatusGroup.PERMANENT),
    CREATE_REQUESTED(StatusGroup.PERMANENT),
    DELETE_REQUESTED(StatusGroup.PERMANENT),
    IN_PROGRESS(StatusGroup.TRANSIENT),
    ZOMBIE(StatusGroup.PERMANENT);

    private final StatusGroup statusGroup;

    InstanceStatus(StatusGroup statusGroup) {
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
