package com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class BootstrapMachinesSuccess extends StackEvent {
    @JsonCreator
    public BootstrapMachinesSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
