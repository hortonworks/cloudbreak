package com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class OrchestratorConfigRequest extends StackEvent {
    public OrchestratorConfigRequest(Long stackId) {
        super(stackId);
    }
}
