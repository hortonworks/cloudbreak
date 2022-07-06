package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StackCreationWaitRequest extends SdxEvent {

    @JsonCreator
    public StackCreationWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static StackCreationWaitRequest from(SdxContext context) {
        return new StackCreationWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "StackCreationWaitRequest";
    }
}
