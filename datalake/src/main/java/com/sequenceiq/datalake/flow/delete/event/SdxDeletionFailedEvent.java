package com.sequenceiq.datalake.flow.delete.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxDeletionFailedEvent extends SdxFailedEvent {

    private final boolean forced;

    @JsonCreator
    public SdxDeletionFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("forced") boolean forced) {
        super(sdxId, userId, exception);
        this.forced = forced;
    }

    public static SdxDeletionFailedEvent from(SdxEvent event, Exception exception, boolean forced) {
        return new SdxDeletionFailedEvent(event.getResourceId(), event.getUserId(), exception, forced);
    }

    public boolean isForced() {
        return forced;
    }

    @Override
    public String selector() {
        return "SdxDeletionFailedEvent";
    }
}
