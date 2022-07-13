package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class RotateSaltPasswordFailureResponse extends StackFailureEvent {

    @JsonCreator
    public RotateSaltPasswordFailureResponse(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
