package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class WaitForClusterManagerFailed extends StackFailureEvent {
    public WaitForClusterManagerFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}