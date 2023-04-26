package com.sequenceiq.datalake.flow.dr.backup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupWaitRequest extends SdxEvent {

    private final String operationId;

    private final int databaseMaxDurationInMin;

    @JsonCreator
    public DatalakeDatabaseBackupWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("databaseMaxDurationInMin") int databaseMaxDurationInMin) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.databaseMaxDurationInMin = databaseMaxDurationInMin;
    }

    public static DatalakeDatabaseBackupWaitRequest from(SdxContext context, String operationId, int databaseMaxDurationInMin) {
        return new DatalakeDatabaseBackupWaitRequest(context.getSdxId(), context.getUserId(), operationId, databaseMaxDurationInMin);
    }

    public String getOperationId() {
        return operationId;
    }

    public int getDatabaseMaxDurationInMin() {
        return databaseMaxDurationInMin;
    }
}
