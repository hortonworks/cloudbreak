package com.sequenceiq.datalake.flow.java.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class WaitSetDatalakeDefaultJavaVersionResult extends SdxEvent {
    @JsonCreator
    public WaitSetDatalakeDefaultJavaVersionResult(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
