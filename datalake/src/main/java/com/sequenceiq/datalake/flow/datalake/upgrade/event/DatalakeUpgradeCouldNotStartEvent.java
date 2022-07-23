package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeUpgradeCouldNotStartEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public DatalakeUpgradeCouldNotStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeUpgradeCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeUpgradeCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "DatalakeUpgradeCouldNotStartEvent";
    }

    public Exception getException() {
        return exception;
    }
}
