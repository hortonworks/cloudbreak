package com.sequenceiq.datalake.flow.dr.backup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupWaitRequest extends SdxEvent {

    private final String operationId;

    @JsonCreator
    public DatalakeDatabaseBackupWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public static DatalakeDatabaseBackupWaitRequest from(SdxContext context, String operationId) {
        return new DatalakeDatabaseBackupWaitRequest(context.getSdxId(), context.getUserId(), operationId);
    }

    public String getOperationId() {
        return operationId;
    }

}
