package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class StartClusterManagerManagementServicesFailed extends StackFailureEvent {
    public StartClusterManagerManagementServicesFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
