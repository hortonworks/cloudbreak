package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FreeIpaTrustCancelFailureEvent extends StackFailureEvent {

    @JsonCreator
    public FreeIpaTrustCancelFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
