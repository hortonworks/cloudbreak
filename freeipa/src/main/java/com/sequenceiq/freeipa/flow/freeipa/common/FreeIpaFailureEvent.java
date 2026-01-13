package com.sequenceiq.freeipa.flow.freeipa.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public abstract class FreeIpaFailureEvent extends StackFailureEvent {

    @JsonCreator
    public FreeIpaFailureEvent(
            @JsonProperty("stackId")Long stackId,
            @JsonProperty("failureType")FailureType failureType,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception, failureType);
    }

    @Override
    public String toString() {
        return "FreeIpaFailureEvent{" +
                super.toString() +
                '}';
    }
}
