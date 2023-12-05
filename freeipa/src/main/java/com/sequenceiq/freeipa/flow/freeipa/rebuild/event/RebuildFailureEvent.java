package com.sequenceiq.freeipa.flow.freeipa.rebuild.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RebuildFailureEvent extends StackEvent {

    private final Exception exception;

    @JsonCreator
    public RebuildFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "RebuildFailureEvent{" +
                "exception=" + exception +
                "} " + super.toString();
    }
}
