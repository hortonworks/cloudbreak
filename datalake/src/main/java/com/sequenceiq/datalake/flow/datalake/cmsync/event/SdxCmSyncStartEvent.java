package com.sequenceiq.datalake.flow.datalake.cmsync.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCmSyncStartEvent extends SdxEvent {
    @JsonCreator
    public SdxCmSyncStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId) {
        super(sdxId, userId);
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxCmSyncStartEvent.class, other);
    }
}
