package com.sequenceiq.datalake.flow.create;

public class SdxFailureEvent extends SdxEvent {

    private final Exception exception;

    public SdxFailureEvent(Long sdxId, Exception exception) {
        super(sdxId);
        this.exception = exception;
    }

    public SdxFailureEvent(String selector, Long sdxId, Exception exception) {
        super(selector, sdxId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
