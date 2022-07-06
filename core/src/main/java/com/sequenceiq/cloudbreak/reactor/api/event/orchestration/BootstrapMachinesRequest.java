package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapMachinesRequest extends StackEvent {

    private final boolean reBootstrap;

    public BootstrapMachinesRequest(Long stackId) {
        super(stackId);
        reBootstrap = false;
    }

    @JsonCreator
    public BootstrapMachinesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("reBootstrap") boolean reBootstrap) {
        super(stackId);
        this.reBootstrap = reBootstrap;
    }

    public boolean isReBootstrap() {
        return reBootstrap;
    }
}
