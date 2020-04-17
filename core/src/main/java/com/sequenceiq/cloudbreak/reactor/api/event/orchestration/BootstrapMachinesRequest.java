package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapMachinesRequest extends StackEvent {

    private boolean reBootstrap;

    public BootstrapMachinesRequest(Long stackId) {
        super(stackId);
    }

    public BootstrapMachinesRequest(Long stackId, boolean reBootstrap) {
        super(stackId);
        this.reBootstrap = reBootstrap;
    }

    public boolean isReBootstrap() {
        return reBootstrap;
    }
}
