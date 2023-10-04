package com.sequenceiq.freeipa.flow.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StackFailureEvent extends StackEvent {

    private final Exception exception;

    public StackFailureEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    @JsonCreator
    public StackFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "StackFailureEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
