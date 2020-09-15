package com.sequenceiq.redbeams.flow.redbeams.common;

public class RedbeamsFailureEvent extends RedbeamsEvent {

    private final Exception exception;

    public RedbeamsFailureEvent(Long resourceId, Exception exception) {
        super(resourceId);
        this.exception = exception;
    }

    public RedbeamsFailureEvent(Long resourceId, Exception exception, boolean force) {
        super(resourceId, force);
        this.exception = exception;
    }

    public RedbeamsFailureEvent(String selector, Long resourceId, Exception exception) {
        super(selector, resourceId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
