package com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class BootstrapMachinesRequest extends StackEvent {
    public BootstrapMachinesRequest(Long stackId) {
        super(stackId);
    }
}
