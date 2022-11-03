package com.sequenceiq.datalake.flow.dr.event;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseDrStartBaseEvent extends SdxEvent  {

    private final SdxOperation drStatus;

    private final List<String> skipDatabaseNames;

    @JsonCreator
    public DatalakeDatabaseDrStartBaseEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("operationType") SdxOperationType operationType,
            @JsonProperty("skipDatabaseNames") List<String> skipDatabaseNames,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        drStatus = new SdxOperation(operationType, sdxId);
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
                                            SdxOperationType operationType, List<String> skipDatabaseNames) {
        super(selector, sdxId, userId);
        drStatus = new SdxOperation(operationType, sdxId);
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String sdxName, String userId,
            SdxOperationType operationType) {
        super(selector, sdxId, sdxName, userId);
        drStatus = new SdxOperation(operationType, sdxId);
        this.skipDatabaseNames = Collections.emptyList();
    }

    public DatalakeDatabaseDrStartBaseEvent(String selector, Long sdxId, String userId,
                                            SdxOperation drStatus, List<String> skipDatabaseNames) {
        super(selector, sdxId, userId);
        this.drStatus = drStatus;
        this.skipDatabaseNames = skipDatabaseNames;
    }

    public SdxOperation getDrStatus() {
        return drStatus;
    }

    public SdxOperationType getOperationType() {
        return drStatus.getOperationType();
    }

    public List<String> getSkipDatabaseNames() {
        return skipDatabaseNames;
    }
}
