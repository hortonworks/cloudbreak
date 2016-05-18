package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class BootstrapMachinesRequest extends StackEvent {
    public BootstrapMachinesRequest(Long stackId) {
        super(stackId);
    }
}
