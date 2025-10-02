package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaTrustSetupFinishFailureEvent extends StackFailureEvent {

    @JsonCreator
    public FreeIpaTrustSetupFinishFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
