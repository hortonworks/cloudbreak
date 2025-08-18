package com.sequenceiq.datalake.flow.dr.restore.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;

public class DatalakeRestoreParams {

    private final String operationId;

    private final SdxOperation drStatus;

    private final String backupLocation;

    private final String backupId;

    private final boolean validationOnly;

    private final int databaseMaxDurationInMin;

    @JsonCreator
    public DatalakeRestoreParams(
            @JsonProperty("operationId") String operationId,
            @JsonProperty("drStatus") SdxOperation drStatus,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @JsonProperty("validationOnly") boolean validationOnly) {
        this.operationId = operationId;
        this.drStatus = drStatus;
        this.backupLocation = backupLocation;
        this.backupId = backupId;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
        this.validationOnly = validationOnly;
    }

    public String getOperationId() {
        return operationId;
    }

    public SdxOperation getDrStatus() {
        return drStatus;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }

    public boolean isValidationOnly() {
        return validationOnly;
    }

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }
}