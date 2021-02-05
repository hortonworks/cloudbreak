package com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class OrchestratorConfigSuccess extends StackEvent {
    public OrchestratorConfigSuccess(Long stackId) {
        super(stackId);
    }
}
