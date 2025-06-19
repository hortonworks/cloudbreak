package com.sequenceiq.freeipa.flow.freeipa.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FinishTrustSetupAddTrustRequest extends StackEvent {

    @JsonCreator
    public FinishTrustSetupAddTrustRequest(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
