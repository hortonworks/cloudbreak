package com.sequenceiq.datalake.flow;

public abstract class SdxFailedEvent extends SdxEvent {

    private final Exception exception;

    public SdxFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "SdxFailedEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
