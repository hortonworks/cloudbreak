package com.sequenceiq.cloudbreak.reactor.api.event;

public class StackFailureEvent extends StackEvent {
    private Exception exception;

    public StackFailureEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    public StackFailureEvent(String selector, Long stackId, Exception exception) {
        super(selector, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
