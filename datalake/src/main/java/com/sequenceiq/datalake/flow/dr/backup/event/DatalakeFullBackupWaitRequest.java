package com.sequenceiq.datalake.flow.dr.backup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeFullBackupWaitRequest extends SdxEvent {

    private final String operationId;

    private final int fullDrMaxDurationInMin;

    @JsonCreator
    public DatalakeFullBackupWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("fullDrMaxDurationInMin") int fullDrMaxDurationInMin) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.fullDrMaxDurationInMin = fullDrMaxDurationInMin;
    }

    public static DatalakeFullBackupWaitRequest from(SdxContext context, String operationId, int fullDrMaxDurationInMin) {
        return new DatalakeFullBackupWaitRequest(context.getSdxId(), context.getUserId(), operationId, fullDrMaxDurationInMin);
    }

    public String getOperationId() {
        return operationId;
    }

    public int getFullDrMaxDurationInMin() {
        return fullDrMaxDurationInMin;
    }
}
