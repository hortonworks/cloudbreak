package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class CrossRealmTrustValidationFailed extends StackFailureEvent {

    @JsonCreator
    public CrossRealmTrustValidationFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
