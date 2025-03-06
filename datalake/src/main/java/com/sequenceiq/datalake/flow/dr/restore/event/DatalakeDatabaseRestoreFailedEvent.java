package com.sequenceiq.datalake.flow.dr.restore.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreFailedEvent extends SdxEvent {

    private final Exception exception;

    @JsonCreator
    public DatalakeDatabaseRestoreFailedEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseRestoreFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseRestoreFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeDatabaseRestoreFailedEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
