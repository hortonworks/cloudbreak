package com.sequenceiq.sdx.api.model;

public class SdxDatabaseBackupStatusResponse {
    private DatalakeDatabaseDrStatus status;

    private String statusReason;

    public SdxDatabaseBackupStatusResponse(DatalakeDatabaseDrStatus status) {
        this.status = status;
    }

    public SdxDatabaseBackupStatusResponse(DatalakeDatabaseDrStatus status, String statusReason) {
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
