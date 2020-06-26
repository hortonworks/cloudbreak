package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseRestoreCouldNotStartEvent extends SdxEvent {
    private final Exception exception;

    public DatalakeDatabaseRestoreCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
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
