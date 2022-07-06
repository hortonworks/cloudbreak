package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionWaitRequest extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public StackDeletionWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    public static StackDeletionWaitRequest from(SdxContext context, SdxDeleteStartEvent payload) {
        return new StackDeletionWaitRequest(context.getSdxId(), context.getUserId(), payload.isForced());
    }

    @Override
    public String selector() {
        return "StackDeletionWaitRequest";
    }

    public boolean isForced() {
        return forced;
    }
}
