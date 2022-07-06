package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackDeletionSuccessEvent extends SdxEvent {

    private final boolean forced;

    @JsonCreator
    public StackDeletionSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId);
        this.forced = forced;
    }

    @Override
    public String selector() {
        return "StackDeletionSuccessEvent";
    }

    public boolean isForced() {
        return forced;
    }
}

