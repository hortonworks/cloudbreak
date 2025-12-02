package com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class RebuildValidateHealthFailed extends StackFailureEvent {
    @JsonCreator
    public RebuildValidateHealthFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, failureType);
    }
}
