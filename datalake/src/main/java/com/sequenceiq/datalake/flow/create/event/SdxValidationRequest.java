package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxValidationRequest extends SdxEvent {

    @JsonCreator
    public SdxValidationRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public SdxValidationRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "SdxValidationRequest";
    }
}
