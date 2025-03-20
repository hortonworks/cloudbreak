package com.sequenceiq.datalake.flow.sku.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitDataLakeSkuMigrationRequest extends SdxEvent {

    private final boolean force;

    @JsonCreator
    public WaitDataLakeSkuMigrationRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("force") boolean force) {
        super(sdxId, userId);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }
}