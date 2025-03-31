package com.sequenceiq.datalake.flow.sku;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DataLakeSkuMigrationTriggerEvent extends SdxEvent {

    private final boolean force;

    public DataLakeSkuMigrationTriggerEvent(String selector, Long sdxId, String userId, boolean force) {
        super(selector, sdxId, userId);
        this.force = force;
    }

    @JsonCreator
    public DataLakeSkuMigrationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("force") boolean force,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, sdxId, userId, accepted);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String toString() {
        return "DataLakeSkuMigrationTriggerEvent{" +
                "force=" + force +
                '}';
    }
}
