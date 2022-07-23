package com.sequenceiq.datalake.flow.dr.restore.event;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreCouldNotStartEvent extends SdxEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    @JsonCreator
    public DatalakeDatabaseRestoreCouldNotStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseRestoreCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseRestoreCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeDatabaseRestoreCouldNotStartEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
