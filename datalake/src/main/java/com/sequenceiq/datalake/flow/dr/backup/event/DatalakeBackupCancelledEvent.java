package com.sequenceiq.datalake.flow.dr.backup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeBackupCancelledEvent extends SdxEvent {
    private final String operationId;

    @JsonCreator
    public DatalakeBackupCancelledEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationId") String operationId) {
        super(sdxId, userId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

}
