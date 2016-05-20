package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class BootstrapMachinesFailed extends StackFailureEvent {
    public BootstrapMachinesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
