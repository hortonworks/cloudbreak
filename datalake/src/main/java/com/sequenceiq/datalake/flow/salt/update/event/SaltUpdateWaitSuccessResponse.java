package com.sequenceiq.datalake.flow.salt.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SaltUpdateWaitSuccessResponse extends SdxEvent {
    @JsonCreator
    public SaltUpdateWaitSuccessResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
