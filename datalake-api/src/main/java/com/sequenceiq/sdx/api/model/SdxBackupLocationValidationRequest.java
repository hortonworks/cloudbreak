package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupLocationValidationRequest {

    @ValidStackNameFormat
    @ValidStackNameLength
    @Schema(description = ModelDescriptions.DATA_LAKE_NAME)
    private String clusterName;

    @Schema(description = ModelDescriptions.BACKUP_LOCATION)
    private String backupLocation;

    @Schema(description = ModelDescriptions.OPERATION_TYPE)
    private BackupOperationType operationType;

    public SdxBackupLocationValidationRequest() {
    }

    public SdxBackupLocationValidationRequest(String clusterName, BackupOperationType operationType) {
        this.clusterName = clusterName;
        this.operationType = operationType;
    }

    public SdxBackupLocationValidationRequest(String clusterName, BackupOperationType operationType, String backupLocation) {
        this.clusterName = clusterName;
        this.operationType = operationType;
        this.backupLocation = backupLocation;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getClusterName() {
        return clusterName;
    }

    public BackupOperationType getOperationType() {
        return operationType;
    }

    @Override
    public String toString() {
        return "SdxBackupLocationValidationRequest{"
                + "ClusterName='" + clusterName + '\''
                + "Operation Type ='" + operationType + '\''
                + "BackupLocation='" + backupLocation + '\''
                + '}';
    }
}
