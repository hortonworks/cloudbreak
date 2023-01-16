package com.sequenceiq.datalake.flow.modifyproxy.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class ModifyProxyConfigWaitRequest extends SdxEvent {

    @JsonCreator
    public ModifyProxyConfigWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(ModifyProxyConfigWaitRequest.class, other);
    }
}
