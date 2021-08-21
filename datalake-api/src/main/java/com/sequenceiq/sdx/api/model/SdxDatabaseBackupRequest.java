package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SdxDatabaseBackupRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseBackupRequest {
    @NotNull
    @ApiModelProperty(value = ModelDescriptions.BACKUP_ID, required = true)
    private String backupId;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.BACKUP_LOCATION, required = true)
    private String backupLocation;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.CLOSE_CONNECTIONS, required = true)
    private boolean closeConnections;

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    public boolean getCloseConnections() {
        return closeConnections;
    }

    public void setCloseConnections(boolean closeConnections) {
        this.closeConnections = closeConnections;
    }

    @Override
    public String toString() {
        return "SdxDatabaseBackupRequest{" +
                "backupId='" + backupId + '\'' +
                ", backupLocation='" + backupLocation + '\'' +
                ", closeConnections=" + closeConnections +
                '}';
    }
}
