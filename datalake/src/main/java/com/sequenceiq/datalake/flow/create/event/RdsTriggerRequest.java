package com.sequenceiq.datalake.flow.create.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RdsTriggerRequest extends SdxEvent {
    @JsonCreator
    public RdsTriggerRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public RdsTriggerRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RdsTriggerRequest.class);
    }
}
