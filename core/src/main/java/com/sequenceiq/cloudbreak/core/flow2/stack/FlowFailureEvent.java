package com.sequenceiq.cloudbreak.core.flow2.stack;

public class FlowFailureEvent extends FlowStackEvent {
    private Exception exception;

    public FlowFailureEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
