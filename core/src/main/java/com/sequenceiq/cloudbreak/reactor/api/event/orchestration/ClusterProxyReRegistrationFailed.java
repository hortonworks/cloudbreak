package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterProxyReRegistrationFailed extends StackFailureEvent {
    public ClusterProxyReRegistrationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
