package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class RotateSaltPasswordFailureResponse extends StackFailureEvent {
    public RotateSaltPasswordFailureResponse(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    @JsonCreator
    public RotateSaltPasswordFailureResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(selector, stackId, exception);
    }
}
