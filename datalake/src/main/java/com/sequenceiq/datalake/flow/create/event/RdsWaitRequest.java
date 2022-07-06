package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RdsWaitRequest extends SdxEvent {

    @JsonCreator
    public RdsWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public RdsWaitRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "RdsWaitRequest";
    }
}
