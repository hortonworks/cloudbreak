package com.sequenceiq.freeipa.flow.freeipa.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FinishTrustSetupAddTrustSuccess extends StackEvent {

    @JsonCreator
    public FinishTrustSetupAddTrustSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
