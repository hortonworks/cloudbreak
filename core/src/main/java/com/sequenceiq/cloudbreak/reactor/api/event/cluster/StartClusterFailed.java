package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StartClusterFailed extends StackFailureEvent {
    public StartClusterFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
