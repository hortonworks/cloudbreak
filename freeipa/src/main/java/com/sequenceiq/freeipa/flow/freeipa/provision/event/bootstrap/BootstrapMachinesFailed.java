package com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class BootstrapMachinesFailed extends StackFailureEvent {
    @JsonCreator
    public BootstrapMachinesFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(stackId, ex);
    }
}
