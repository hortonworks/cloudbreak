package com.sequenceiq.freeipa.flow.freeipa.trust.finish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FinishTrustSetupAddTrustFailed extends FinishTrustSetupFailureEvent {

    @JsonCreator
    public FinishTrustSetupAddTrustFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
