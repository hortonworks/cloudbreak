package com.sequenceiq.datalake.flow.dr.backup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupCouldNotStartEvent extends SdxEvent {

    private final Exception exception;

    @JsonCreator
    public DatalakeDatabaseBackupCouldNotStartEvent(
            @JsonProperty("resourceId") Long sdxId,
            @JsonProperty("userId") String userId,
            @JsonProperty("exception") Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseBackupCouldNotStartEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseBackupCouldNotStartEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeDatabaseBackupCouldNotStartEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
