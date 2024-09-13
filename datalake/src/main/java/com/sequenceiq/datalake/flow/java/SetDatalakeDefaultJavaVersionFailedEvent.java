package com.sequenceiq.datalake.flow.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SetDatalakeDefaultJavaVersionFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public SetDatalakeDefaultJavaVersionFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}