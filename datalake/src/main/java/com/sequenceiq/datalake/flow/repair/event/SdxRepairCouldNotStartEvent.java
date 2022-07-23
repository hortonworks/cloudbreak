package com.sequenceiq.datalake.flow.repair.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairCouldNotStartEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public SdxRepairCouldNotStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxRepairCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new SdxRepairCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxRepairCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }
}
