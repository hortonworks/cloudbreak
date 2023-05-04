package com.sequenceiq.datalake.flow.dr.backup.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeBackupAwaitServicesStoppedRequest extends SdxEvent {
    private final String operationId;

    private final SdxOperation drStatus;

    private final String backupLocation;

    private final List<String> skipDatabaseNames;

    @JsonCreator
    public DatalakeBackupAwaitServicesStoppedRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("drStatus") SdxOperation drStatus,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("skipDatabaseNames") List<String> skipDatabaseNames) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.drStatus = drStatus;
        this.backupLocation = backupLocation;
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public static DatalakeBackupAwaitServicesStoppedRequest from(DatalakeDatabaseBackupStartEvent startEvent) {
        return new DatalakeBackupAwaitServicesStoppedRequest(
                startEvent.getResourceId(),
                startEvent.getUserId(),
                startEvent.getDrStatus(),
                startEvent.getBackupRequest().getBackupId(),
                startEvent.getBackupRequest().getBackupLocation(),
                startEvent.getSkipDatabaseNames()
        );
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

    public List<String> getSkipDatabaseNames() {
        return skipDatabaseNames;
    }
}
