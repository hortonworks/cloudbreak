package com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class BootstrapMachinesFailed extends StackFailureEvent {
    public BootstrapMachinesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
