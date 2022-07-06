package com.sequenceiq.datalake.flow.stop.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxStopAllDatahubRequest extends SdxEvent {

    @JsonCreator
    public SdxStopAllDatahubRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public static SdxStopAllDatahubRequest from(SdxContext context) {
        return new SdxStopAllDatahubRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxStopAllDatahubRequest";
    }

}
