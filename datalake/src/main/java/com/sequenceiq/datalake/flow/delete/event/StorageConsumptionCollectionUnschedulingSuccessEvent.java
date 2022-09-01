package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StorageConsumptionCollectionUnschedulingSuccessEvent extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public StorageConsumptionCollectionUnschedulingSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public boolean isForced() {
        return forced;
    }

}
