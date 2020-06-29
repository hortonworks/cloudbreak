package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupCouldNotStartEvent extends SdxEvent {
    private final Exception exception;

    public DatalakeDatabaseBackupCouldNotStartEvent(Long sdxId, String userId, Exception exception) {
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
