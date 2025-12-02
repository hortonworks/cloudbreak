package com.sequenceiq.freeipa.flow.freeipa.rebuild.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RebuildFailureEvent extends StackEvent {

    private final Exception exception;

    private final FailureType failureType;

    @JsonCreator
    public RebuildFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failureType") FailureType failureType,
            @JsonProperty("exception") Exception exception
    ) {
        super(stackId);
        this.exception = exception;
        this.failureType = failureType;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public FailureType getFailureType() {
        return failureType == null ? FailureType.ERROR : failureType;
    }

    @Override
    public String toString() {
        return "RebuildFailureEvent{" +
                "exception=" + exception +
                "failureType=" + failureType +
                "} " + super.toString();
    }
}
