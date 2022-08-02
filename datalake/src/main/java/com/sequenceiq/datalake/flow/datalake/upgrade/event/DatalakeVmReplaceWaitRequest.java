package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeVmReplaceWaitRequest extends SdxEvent {

    @JsonCreator
    public DatalakeVmReplaceWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    public DatalakeVmReplaceWaitRequest(SdxContext context) {
        super(context);
    }

    @Override
    public String toString() {
        return "DatalakeVmReplaceWaitRequest{} " + super.toString();
    }
}
