package com.sequenceiq.datalake.flow.trustedrealm.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpdateTrustedRealmWaitRequest extends SdxEvent {

    @JsonCreator
    public UpdateTrustedRealmWaitRequest(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(UpdateTrustedRealmWaitRequest.class, other);
    }
}
