package com.sequenceiq.datalake.flow.salt.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SaltUpdateWaitRequest extends SdxEvent {
    @JsonCreator
    public SaltUpdateWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
