package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpdateClusterConfigFailed extends StackFailureEvent {
    public UpdateClusterConfigFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}