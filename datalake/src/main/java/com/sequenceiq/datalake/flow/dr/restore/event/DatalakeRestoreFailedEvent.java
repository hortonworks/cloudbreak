package com.sequenceiq.datalake.flow.dr.restore.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRestoreFailedEvent extends SdxEvent {

    private final Exception exception;

    public DatalakeRestoreFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static DatalakeRestoreFailedEvent from(SdxEvent event, Exception exception) {
        return new DatalakeRestoreFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "DatalakeRestoreFailedEvent{" +
                "exception= " + exception.toString() +
                '}';
    }
}
