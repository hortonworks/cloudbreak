package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RemoveHostsFromOrchestrationSuccess extends StackEvent {

    public RemoveHostsFromOrchestrationSuccess(Long stackId) {
        super(stackId);
    }
}
