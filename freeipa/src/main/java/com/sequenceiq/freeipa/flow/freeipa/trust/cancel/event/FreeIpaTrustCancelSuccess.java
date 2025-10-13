package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaTrustCancelSuccess extends StackEvent {

    @JsonCreator
    public FreeIpaTrustCancelSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
