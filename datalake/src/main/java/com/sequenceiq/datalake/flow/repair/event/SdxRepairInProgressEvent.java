package com.sequenceiq.datalake.flow.repair.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairInProgressEvent extends SdxEvent {

    @JsonCreator
    public SdxRepairInProgressEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxRepairInProgressEvent";
    }
}
