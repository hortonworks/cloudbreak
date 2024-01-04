package com.sequenceiq.sdx.api.model;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SdxDatabaseBackupRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseBackupRequest {

    @NotNull
    @Schema(description = ModelDescriptions.BACKUP_ID, required = true)
    private String backupId;

    @NotNull
    @Schema(description = ModelDescriptions.BACKUP_LOCATION, required = true)
    private String backupLocation;

    @NotNull
    @Schema(description = ModelDescriptions.CLOSE_CONNECTIONS, required = true)
    private boolean closeConnections;

    @Schema(description = ModelDescriptions.SKIP_DATABASE_NAMES, required = false)
    private List<String> skipDatabaseNames;

    @Schema(description = ModelDescriptions.DATABASE_BACKUP_RESTORE_MAX_DURATION, required = false)
    private int databaseMaxDurationInMin;

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

    public boolean isCloseConnections() {
        return closeConnections;
    }

    public void setCloseConnections(boolean closeConnections) {
        this.closeConnections = closeConnections;
    }

    public List<String> getSkipDatabaseNames() {
        return skipDatabaseNames == null ? Collections.emptyList() : skipDatabaseNames;
    }

    public void setSkipDatabaseNames(List<String> skipDatabaseNames) {
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }

    public void setDatabaseMaxDurationInMin(int databaseMaxDurationInMin) {
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
    }

    @Override
    public String toString() {
        return "SdxDatabaseBackupRequest{" +
                "backupId='" + backupId + '\'' +
                ", backupLocation='" + backupLocation + '\'' +
                ", closeConnections=" + closeConnections +
                ", skipDatabaseNames='" + skipDatabaseNames + '\'' +
                ", databaseMaxDurationInMin='" + databaseMaxDurationInMin + '\'' +
                '}';
    }
}
