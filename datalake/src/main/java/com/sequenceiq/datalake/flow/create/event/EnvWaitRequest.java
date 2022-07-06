package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class EnvWaitRequest extends SdxEvent {

    @JsonCreator
    public EnvWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static EnvWaitRequest from(SdxContext context) {
        return new EnvWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }
}
