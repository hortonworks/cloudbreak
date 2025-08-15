package com.sequenceiq.sdx.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.sdx.validation.ValidLinuxDirectoryPath;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SdxDatabaseBackupRestoreSettingsRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxBackupRestoreSettingsRequest {

    @ValidLinuxDirectoryPath
    @Schema(description = ModelDescriptions.BACKUP_TEMP_LOCATION)
    private String backupTempLocation;

    @Min(value = 0, message = "Value must be at least 0")
    @Max(value = 1440, message = "Value must not exceed 1440")
    @Schema(description = ModelDescriptions.BACKUP_TIMEOUT_IN_MINUTES)
    private Integer backupTimeoutInMinutes;

    @ValidLinuxDirectoryPath
    @Schema(description = ModelDescriptions.RESTORE_TEMP_LOCATION)
    private String restoreTempLocation;

    @Min(value = 0, message = "Value must be at least 0")
    @Max(value = 1440, message = "Value must not exceed 1440")
    @Schema(description = ModelDescriptions.RESTORE_TIMEOUT_IN_MINUTES)
    private Integer restoreTimeoutInMinutes;

    public String getBackupTempLocation() {
        return backupTempLocation;
    }

    public void setBackupTempLocation(String backupTempLocation) {
        this.backupTempLocation = backupTempLocation;
    }

    public Integer getBackupTimeoutInMinutes() {
        return backupTimeoutInMinutes;
    }

    public void setBackupTimeoutInMinutes(Integer backupTimeoutInMinutes) {
        this.backupTimeoutInMinutes = backupTimeoutInMinutes;
    }

    public String getRestoreTempLocation() {
        return restoreTempLocation;
    }

    public void setRestoreTempLocation(String restoreTempLocation) {
        this.restoreTempLocation = restoreTempLocation;
    }

    public Integer getRestoreTimeoutInMinutes() {
        return restoreTimeoutInMinutes;
    }

    public void setRestoreTimeoutInMinutes(Integer restoreTimeoutInMinutes) {
        this.restoreTimeoutInMinutes = restoreTimeoutInMinutes;
    }

    @Override
    public String toString() {
        return "SdxDatabaseBackupRestoreSettingsRequest{" +
                ", backupTempLocation='" + backupTempLocation + '\'' +
                ", backupTimeoutInMinutes=" + backupTimeoutInMinutes +
                ", restoreTempLocation='" + restoreTempLocation + '\'' +
                ", restoreTimeoutInMinutes=" + restoreTimeoutInMinutes +
                '}';
    }
}
