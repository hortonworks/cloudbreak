package com.sequenceiq.datalake.flow.repair.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairWaitRequest extends SdxEvent {

    @JsonCreator
    public SdxRepairWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static SdxRepairWaitRequest from(SdxContext context) {
        return new SdxRepairWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxRepairWaitRequest";
    }

}

