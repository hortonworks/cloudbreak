package com.sequenceiq.datalake.flow.datahub.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class StartDatahubFailedEvent extends SdxFailedEvent {
    @JsonCreator
    public StartDatahubFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId, exception);
    }

    public static StartDatahubFailedEvent from(SdxEvent event, Exception exception) {
        return new StartDatahubFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "StartDatahubFailedEvent";
    }
}
