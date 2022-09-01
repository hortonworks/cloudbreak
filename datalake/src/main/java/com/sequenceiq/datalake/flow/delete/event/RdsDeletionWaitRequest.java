package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsDeletionWaitRequest extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public RdsDeletionWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public static RdsDeletionWaitRequest from(SdxContext context, StorageConsumptionCollectionUnschedulingSuccessEvent payload) {
        return new RdsDeletionWaitRequest(context.getSdxId(), context.getUserId(), payload.isForced());
    }

    @Override
    public String selector() {
        return "RdsDeletionWaitRequest";
    }

    public boolean isForced() {
        return forced;
    }

}
