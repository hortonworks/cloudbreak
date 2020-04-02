package com.sequenceiq.redbeams.api.model.common;

public enum DetailedDBStackStatus {
    UNKNOWN(null),
    // Provision statuses
    PROVISION_REQUESTED(Status.REQUESTED),
    CREATING_INFRASTRUCTURE(Status.CREATE_IN_PROGRESS),
    PROVISIONED(Status.AVAILABLE),
    PROVISION_FAILED(Status.CREATE_FAILED),
    // Termination statuses
    DELETE_REQUESTED(Status.DELETE_REQUESTED),
    DELETE_IN_PROGRESS(Status.DELETE_IN_PROGRESS),
    DEREGISTERING(Status.DELETE_IN_PROGRESS),
    DELETE_COMPLETED(Status.DELETE_COMPLETED),
    DELETE_FAILED(Status.DELETE_FAILED),
    // The stack is available
    AVAILABLE(Status.AVAILABLE),
    // Wait for sync
    WAIT_FOR_SYNC(Status.WAIT_FOR_SYNC),
    //Start statuses
    START_REQUESTED(Status.START_REQUESTED),
    START_IN_PROGRESS(Status.START_IN_PROGRESS),
    START_FAILED(Status.START_FAILED),
    STARTED(Status.AVAILABLE),
    //Stop statuses
    STOP_REQUESTED(Status.STOP_REQUESTED),
    STOP_IN_PROGRESS(Status.STOP_IN_PROGRESS),
    STOP_FAILED(Status.STOP_FAILED),
    STOPPED(Status.STOPPED);

    private final Status status;

    DetailedDBStackStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
