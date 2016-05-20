package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapMachinesSuccess extends StackEvent {
    public BootstrapMachinesSuccess(Long stackId) {
        super(stackId);
    }
}
