package com.sequenceiq.datalake.flow.atlas.updated.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class CheckAtlasUpdatedFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public CheckAtlasUpdatedFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("Exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static CheckAtlasUpdatedFailedEvent from(SdxEvent event, Exception exception) {
        return new CheckAtlasUpdatedFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }
}
