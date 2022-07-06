package com.sequenceiq.datalake.flow.start.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsStartSuccessEvent extends SdxEvent {

    @JsonCreator
    public RdsStartSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsStartSuccessEvent.class.getSimpleName();
    }
}
