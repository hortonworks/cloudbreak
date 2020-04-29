package com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ClusterProxyUpdateRegistrationFailed extends StackFailureEvent {
    public ClusterProxyUpdateRegistrationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
