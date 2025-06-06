package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareCrossRealmTrustSuccess extends StackEvent {

    @JsonCreator
    public PrepareCrossRealmTrustSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
