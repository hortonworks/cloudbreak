package com.sequenceiq.datalake.flow.modifyproxy.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class ModifyProxyConfigFailureResponse extends SdxFailedEvent {
    @JsonCreator
    public ModifyProxyConfigFailureResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}
