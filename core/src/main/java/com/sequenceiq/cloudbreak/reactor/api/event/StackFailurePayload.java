package com.sequenceiq.cloudbreak.reactor.api.event;

public class StackFailurePayload extends StackPayload {
    private Exception exception;

    public StackFailurePayload(String selector, Long stackId, Exception exception) {
        super(selector, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
