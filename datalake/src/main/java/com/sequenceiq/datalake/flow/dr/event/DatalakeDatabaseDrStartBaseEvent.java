package com.sequenceiq.datalake.flow.dr.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;

import reactor.rx.Promise;

public class DatalakeDatabaseDrStartBaseEvent extends SdxEvent  {

    private final SdxOperation drStatus;

    @JsonCreator
    public DatalakeDatabaseDrStartBaseEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationType") SdxOperationType operationType,
            @JsonIgnoreDeserialization Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        drStatus = new SdxOperation(operationType, sdxId);
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
                                            SdxOperationType operationType) {
        super(selector, sdxId, userId);
        drStatus = new SdxOperation(operationType, sdxId);
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String sdxName, String userId,
            SdxOperationType operationType) {
        super(selector, sdxId, sdxName, userId);
        drStatus = new SdxOperation(operationType, sdxId);
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
                                            SdxOperation drStatus) {
        super(selector, sdxId, userId);
        this.drStatus = drStatus;
    }

    public SdxOperation getDrStatus() {
        return drStatus;
    }

    public SdxOperationType getOperationType() {
        return drStatus.getOperationType();
    }
}
