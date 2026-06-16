package com.sequenceiq.datalake.flow.trustedrealm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class UpdateTrustedRealmFailureResponse extends SdxFailedEvent {

    @JsonCreator
    public UpdateTrustedRealmFailureResponse(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }
}
