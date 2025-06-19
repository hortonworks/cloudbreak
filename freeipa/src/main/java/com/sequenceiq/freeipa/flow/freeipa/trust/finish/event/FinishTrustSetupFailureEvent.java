package com.sequenceiq.freeipa.flow.freeipa.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class FinishTrustSetupFailureEvent extends StackFailureEvent {

    @JsonCreator
    public FinishTrustSetupFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
