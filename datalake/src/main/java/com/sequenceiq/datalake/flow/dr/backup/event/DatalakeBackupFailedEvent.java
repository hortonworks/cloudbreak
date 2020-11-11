package com.sequenceiq.datalake.flow.dr.backup.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeBackupFailedEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeBackupFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeBackupFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeBackupFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeBackupFailedEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
