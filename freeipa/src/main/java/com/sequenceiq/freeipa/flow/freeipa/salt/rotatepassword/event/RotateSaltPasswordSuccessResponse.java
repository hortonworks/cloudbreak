package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RotateSaltPasswordSuccessResponse extends StackEvent {

    @JsonCreator
    public RotateSaltPasswordSuccessResponse(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
