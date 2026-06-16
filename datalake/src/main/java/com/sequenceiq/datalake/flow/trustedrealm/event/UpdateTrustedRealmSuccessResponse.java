package com.sequenceiq.datalake.flow.trustedrealm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpdateTrustedRealmSuccessResponse extends SdxEvent {

    @JsonCreator
    public UpdateTrustedRealmSuccessResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }
}
