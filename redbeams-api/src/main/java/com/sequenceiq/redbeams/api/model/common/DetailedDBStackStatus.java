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
    PRE_DELETE_IN_PROGRESS(Status.PRE_DELETE_IN_PROGRESS),
    DELETE_IN_PROGRESS(Status.DELETE_IN_PROGRESS),
    DELETE_COMPLETED(Status.DELETE_COMPLETED),
    DELETE_FAILED(Status.DELETE_FAILED),
    // The stack is available
    AVAILABLE(Status.AVAILABLE),
    // Wait for sync
    WAIT_FOR_SYNC(Status.WAIT_FOR_SYNC);

    private final Status status;

    DetailedDBStackStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
