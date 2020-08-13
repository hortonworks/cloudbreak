package com.sequenceiq.flow.core;

import com.sequenceiq.cloudbreak.event.CancellationToken;

public class FlowCancellationToken implements CancellationToken {

    private final Flow flow;

    FlowCancellationToken(Flow flow) {
        this.flow = flow;
    }

    @Override
    public boolean isCancelled() {
        return flow.isFlowStopped();
    }
}
