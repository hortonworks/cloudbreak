package com.sequenceiq.datalake.flow;

import com.sequenceiq.datalake.flow.sync.event.SdxSyncFailedEvent;

public abstract class SdxFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public static SdxSyncFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxSyncFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    public Exception getException() {
        return exception;
    }
}
