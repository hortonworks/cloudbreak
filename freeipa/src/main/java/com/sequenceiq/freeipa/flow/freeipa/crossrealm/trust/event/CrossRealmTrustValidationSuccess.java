package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CrossRealmTrustValidationSuccess extends StackEvent {

    @JsonCreator
    public CrossRealmTrustValidationSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
