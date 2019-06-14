package com.sequenceiq.redbeams.flow.redbeams.provision;

public class RedbeamsFailureEvent extends RedbeamsEvent {

    private final Exception exception;

    public RedbeamsFailureEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    public RedbeamsFailureEvent(String selector, Long stackId, Exception exception) {
        super(selector, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
