package com.sequenceiq.datalake.flow.dr.restore.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestoreAwaitServicesStoppedRequest extends SdxEvent {
    private final String operationId;

    private final SdxOperation drStatus;

    private final String backupLocation;

    private final String backupId;

    @JsonCreator
    public DatalakeRestoreAwaitServicesStoppedRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("drStatus") SdxOperation drStatus,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupId") String backupId) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.drStatus = drStatus;
        this.backupLocation = backupLocation;
        this.backupId = backupId;
    }

    public static DatalakeRestoreAwaitServicesStoppedRequest from(DatalakeDatabaseRestoreStartEvent startEvent) {
        return new DatalakeRestoreAwaitServicesStoppedRequest(
                startEvent.getResourceId(),
                startEvent.getUserId(),
                startEvent.getDrStatus(),
                startEvent.getRestoreId(),
                startEvent.getBackupLocation(),
                startEvent.getBackupId()
        );
    }

    public SdxOperation getDrStatus() {
        return drStatus;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupId() {
        return backupId;
    }
}
