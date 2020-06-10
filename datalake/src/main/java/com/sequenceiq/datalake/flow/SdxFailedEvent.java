package com.sequenceiq.datalake.flow;

public abstract class SdxFailedEvent extends SdxEvent {

    private Exception exception;

    public SdxFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
