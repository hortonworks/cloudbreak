package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class EphemeralClusterUpdateFailed extends StackFailureEvent {
    public EphemeralClusterUpdateFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
