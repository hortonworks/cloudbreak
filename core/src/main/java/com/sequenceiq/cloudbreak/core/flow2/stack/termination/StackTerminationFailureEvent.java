package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StackTerminationFailureEvent extends StackFailureEvent {

    @JsonCreator
    public StackTerminationFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }

    @Override
    public String toString() {
        return "StackTerminationFailureEvent{} " + super.toString();
    }
}
