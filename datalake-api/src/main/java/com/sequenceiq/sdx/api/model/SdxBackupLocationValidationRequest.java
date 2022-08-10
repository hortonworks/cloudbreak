package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupLocationValidationRequest {

    @ValidStackNameFormat
    @ValidStackNameLength
    @ApiModelProperty(ModelDescriptions.DATA_LAKE_NAME)
    private String clusterName;

    @ApiModelProperty(value = ModelDescriptions.BACKUP_LOCATION)
    private String backupLocation;

    public SdxBackupLocationValidationRequest() {
    }

    public SdxBackupLocationValidationRequest(String clusterName) {
        this.clusterName = clusterName;
    }

    public SdxBackupLocationValidationRequest(String clusterName, String backupLocation) {
        this.clusterName = clusterName;
        this.backupLocation = backupLocation;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getClusterName() {
        return clusterName;
    }

    @Override
    public String toString() {
        return "SdxBackupLocationValidationRequest{" +
                "ClusterName='" + clusterName + '\'' +
                "BackupLocation='" + backupLocation + '\'' +
                '}';
    }
}