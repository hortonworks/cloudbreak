package com.sequenceiq.datalake.flow.datalake.recovery.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoveryCouldNotStartEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public DatalakeRecoveryCouldNotStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeRecoveryCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRecoveryCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeRecoveryCouldNotStartEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
