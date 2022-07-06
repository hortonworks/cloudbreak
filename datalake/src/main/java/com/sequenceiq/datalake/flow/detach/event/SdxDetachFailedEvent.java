package com.sequenceiq.datalake.flow.detach.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDetachFailedEvent extends SdxFailedEvent {

    @JsonCreator
    public SdxDetachFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception")  Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxDetachFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxDetachFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }
}
