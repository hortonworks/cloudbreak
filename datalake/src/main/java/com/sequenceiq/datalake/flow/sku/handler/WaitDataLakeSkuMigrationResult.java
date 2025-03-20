package com.sequenceiq.datalake.flow.sku.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitDataLakeSkuMigrationResult extends SdxEvent {
    @JsonCreator
    public WaitDataLakeSkuMigrationResult(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
