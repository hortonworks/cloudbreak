package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class StorageValidationRequest extends SdxEvent {

    @JsonCreator
    public StorageValidationRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("sdxName") String sdxName,
            @JsonProperty("userId") String userId) {
        super(selector, sdxId, sdxName, userId);
    }

    public StorageValidationRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return "StorageValidationRequest";
    }
}
