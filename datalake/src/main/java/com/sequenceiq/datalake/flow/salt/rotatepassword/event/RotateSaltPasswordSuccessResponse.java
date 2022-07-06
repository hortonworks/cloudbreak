package com.sequenceiq.datalake.flow.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class RotateSaltPasswordSuccessResponse extends SdxEvent {
    @JsonCreator
    public RotateSaltPasswordSuccessResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
