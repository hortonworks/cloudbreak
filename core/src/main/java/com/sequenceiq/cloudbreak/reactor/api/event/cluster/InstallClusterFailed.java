package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class InstallClusterFailed extends StackFailureEvent {
    public InstallClusterFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
