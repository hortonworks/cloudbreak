package com.sequenceiq.sdx.api.model;

public class SdxDatabaseRestoreStatusResponse {
    private DatalakeDatabaseDrStatus status;

    private String statusReason;

    public SdxDatabaseRestoreStatusResponse(DatalakeDatabaseDrStatus status) {
        this.status = status;
    }

    public SdxDatabaseRestoreStatusResponse(DatalakeDatabaseDrStatus status, String statusReason) {
        this.status = status;
        this.statusReason = statusReason;

    }

    public DatalakeDatabaseDrStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }
}
