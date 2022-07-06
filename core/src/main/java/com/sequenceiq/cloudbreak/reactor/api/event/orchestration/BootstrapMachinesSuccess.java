package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapMachinesSuccess extends StackEvent {
    @JsonCreator
    public BootstrapMachinesSuccess(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
