package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsTriggeredEvent extends SdxEvent {
    @JsonCreator
    public RdsTriggeredEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "RdsTriggeredEvent";
    }
}
