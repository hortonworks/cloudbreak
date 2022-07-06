package com.sequenceiq.datalake.flow.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class RotateSaltPasswordFailureResponse extends SdxFailedEvent {
    @JsonCreator
    public RotateSaltPasswordFailureResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}
