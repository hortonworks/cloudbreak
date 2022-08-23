package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StorageConsumptionCollectionUnschedulingRequest extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public StorageConsumptionCollectionUnschedulingRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public static StorageConsumptionCollectionUnschedulingRequest from(SdxContext context, StackDeletionSuccessEvent payload) {
        return new StorageConsumptionCollectionUnschedulingRequest(context.getSdxId(), context.getUserId(), payload.isForced());
    }

    public boolean isForced() {
        return forced;
    }

}
