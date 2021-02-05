package com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class OrchestratorConfigFailed extends StackFailureEvent {
    public OrchestratorConfigFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
