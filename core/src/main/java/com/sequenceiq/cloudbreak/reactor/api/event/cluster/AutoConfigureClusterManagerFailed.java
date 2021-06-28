package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class AutoConfigureClusterManagerFailed extends StackFailureEvent {
    public AutoConfigureClusterManagerFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
