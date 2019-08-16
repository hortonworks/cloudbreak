package com.sequenceiq.datalake.flow;

public class SdxFailureEvent extends SdxEvent {

    private final Exception exception;

    public SdxFailureEvent(Long sdxId, String userId, String requestId, String sdxCrn, Exception exception) {
        super(sdxId, userId, requestId);
        this.exception = exception;
    }

    public SdxFailureEvent(String selector, Long sdxId, String userId, String requestId,  String sdxCrn, Exception exception) {
        super(selector, sdxId, userId, requestId);
        this.exception = exception;
    }

    public SdxFailureEvent(String selector, SdxContext context, Exception exception) {
        super(selector, context);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
