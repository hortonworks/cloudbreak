package com.sequenceiq.freeipa.flow.stack.update.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class UserDataUpdateFailed extends StackFailureEvent {
    public UserDataUpdateFailed(Long stackId, Exception exception, FailureType failureType) {
        super(stackId, exception, failureType);
    }

    @JsonCreator
    public UserDataUpdateFailed(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(selector, stackId, exception, failureType);
    }
}
