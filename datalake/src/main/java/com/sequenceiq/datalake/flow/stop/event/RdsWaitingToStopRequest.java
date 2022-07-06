package com.sequenceiq.datalake.flow.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsWaitingToStopRequest extends SdxEvent {

    @JsonCreator
    public RdsWaitingToStopRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return RdsWaitingToStopRequest.class.getSimpleName();
    }
}
