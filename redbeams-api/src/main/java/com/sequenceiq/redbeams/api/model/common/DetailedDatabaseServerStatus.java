package com.sequenceiq.redbeams.api.model.common;

public enum DetailedDatabaseServerStatus {
    UNKNOWN(null),
    PROVISIONING(Status.CREATE_IN_PROGRESS),
    PROVISIONED(Status.AVAILABLE),
    PROVISION_FAILED(Status.CREATE_FAILED),
    DELETE_IN_PROGRESS(Status.DELETE_IN_PROGRESS),
    DELETE_COMPLETED(Status.DELETE_COMPLETED),
    DELETE_FAILED(Status.DELETE_FAILED);

    private final Status status;

    DetailedDatabaseServerStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
