package com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class BootstrapMachinesRequest extends StackEvent {
    @JsonCreator
    public BootstrapMachinesRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
