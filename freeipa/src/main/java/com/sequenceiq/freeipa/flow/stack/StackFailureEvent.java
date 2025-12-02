package com.sequenceiq.freeipa.flow.stack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;

public class StackFailureEvent extends StackEvent {

    private final Exception exception;

    private final FailureType failureType;

    public StackFailureEvent(Long stackId, Exception exception, FailureType failureType) {
        super(stackId);
        this.exception = exception;
        this.failureType = failureType;
    }

    @JsonCreator
    public StackFailureEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(selector, stackId);
        this.exception = exception;
        this.failureType = failureType;
    }

    public FailureType getFailureType() {
        return failureType == null ? FailureType.ERROR : failureType;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "StackFailureEvent{" +
                "exception=" + exception +
                "failureType=" + failureType +
                "} " + super.toString();
    }
}
