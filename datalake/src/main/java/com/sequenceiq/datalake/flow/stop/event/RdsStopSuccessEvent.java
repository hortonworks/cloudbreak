package com.sequenceiq.datalake.flow.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsStopSuccessEvent extends SdxEvent {

    @JsonCreator
    public RdsStopSuccessEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsStopSuccessEvent.class.getSimpleName();
    }
}
