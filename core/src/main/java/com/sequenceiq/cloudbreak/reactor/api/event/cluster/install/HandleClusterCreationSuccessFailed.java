package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class HandleClusterCreationSuccessFailed extends StackFailureEvent {
    public HandleClusterCreationSuccessFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
