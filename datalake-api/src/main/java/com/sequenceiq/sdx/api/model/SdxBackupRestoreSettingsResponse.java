package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SdxDatabaseBackupRestoreSettingsResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupRestoreSettingsResponse {

    @Schema(description = ModelDescriptions.BACKUP_TEMP_LOCATION)
    private String backupTempLocation;

    @Schema(description = ModelDescriptions.BACKUP_TIMEOUT_IN_MINUTES)
    private int backupTimeoutInMinutes;

    @Schema(description = ModelDescriptions.RESTORE_TEMP_LOCATION)
    private String restoreTempLocation;

    @Schema(description = ModelDescriptions.RESTORE_TIMEOUT_IN_MINUTES)
    private int restoreTimeoutInMinutes;

    public String getBackupTempLocation() {
        return backupTempLocation;
    }

    public void setBackupTempLocation(String backupTempLocation) {
        this.backupTempLocation = backupTempLocation;
    }

    public int getBackupTimeoutInMinutes() {
        return backupTimeoutInMinutes;
    }

    public void setBackupTimeoutInMinutes(int backupTimeoutInMinutes) {
        this.backupTimeoutInMinutes = backupTimeoutInMinutes;
    }

    public String getRestoreTempLocation() {
        return restoreTempLocation;
    }

    public void setRestoreTempLocation(String restoreTempLocation) {
        this.restoreTempLocation = restoreTempLocation;
    }

    public int getRestoreTimeoutInMinutes() {
        return restoreTimeoutInMinutes;
    }

    public void setRestoreTimeoutInMinutes(int restoreTimeoutInMinutes) {
        this.restoreTimeoutInMinutes = restoreTimeoutInMinutes;
    }

    @Override
    public String toString() {
        return "SdxDatabaseBackupRestoreSettingsResponse{" +
                "backupTempLocation='" + backupTempLocation + '\'' +
                ", backupTimeoutInMinutes=" + backupTimeoutInMinutes +
                ", restoreTempLocation='" + restoreTempLocation + '\'' +
                ", restoreTimeoutInMinutes=" + restoreTimeoutInMinutes +
                '}';
    }
}
