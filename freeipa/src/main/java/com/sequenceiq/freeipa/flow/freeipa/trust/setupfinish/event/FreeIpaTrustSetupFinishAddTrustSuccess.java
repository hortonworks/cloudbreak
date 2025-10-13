package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaTrustSetupFinishAddTrustSuccess extends StackEvent {

    @JsonCreator
    public FreeIpaTrustSetupFinishAddTrustSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
