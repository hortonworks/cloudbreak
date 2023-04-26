package com.sequenceiq.datalake.flow.dr.restore.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeFullRestoreWaitRequest extends SdxEvent {

    private final String operationId;

    private final int fullDrMaxDurationInMin;

    @JsonCreator
    public DatalakeFullRestoreWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("fullDrMaxDurationInMin") int fullDrMaxDurationInMin) {
        super(sdxId, userId);
        this.operationId = operationId;
        this.fullDrMaxDurationInMin = fullDrMaxDurationInMin;
    }

    public static DatalakeFullRestoreWaitRequest from(SdxContext context, String operationId, int fullDrMaxDurationInMin) {
        return new DatalakeFullRestoreWaitRequest(context.getSdxId(), context.getUserId(), operationId, fullDrMaxDurationInMin);
    }

    public String getOperationId() {
        return operationId;
    }

    public int getFullDrMaxDurationInMin() {
        return fullDrMaxDurationInMin;
    }
}
