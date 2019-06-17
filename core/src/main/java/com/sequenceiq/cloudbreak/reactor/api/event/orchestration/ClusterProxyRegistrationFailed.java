package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterProxyRegistrationFailed extends StackFailureEvent {
    public ClusterProxyRegistrationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
