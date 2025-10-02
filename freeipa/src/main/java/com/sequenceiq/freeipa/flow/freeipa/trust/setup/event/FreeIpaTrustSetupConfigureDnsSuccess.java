package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaTrustSetupConfigureDnsSuccess extends StackEvent {

    @JsonCreator
    public FreeIpaTrustSetupConfigureDnsSuccess(@JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
