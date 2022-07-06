package com.sequenceiq.datalake.flow.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStartSuccessEvent extends SdxEvent {

    @JsonCreator
    public SdxStartSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxStartSuccessEvent";
    }
}
