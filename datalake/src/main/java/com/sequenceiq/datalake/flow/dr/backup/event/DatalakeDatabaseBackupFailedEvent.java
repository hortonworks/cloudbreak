package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeDatabaseBackupFailedEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeDatabaseBackupFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeDatabaseBackupFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeDatabaseBackupFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeDatabaseBackupFailedEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
