package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RotateSaltPasswordSuccessResponse extends StackEvent {
    public RotateSaltPasswordSuccessResponse(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public RotateSaltPasswordSuccessResponse(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
