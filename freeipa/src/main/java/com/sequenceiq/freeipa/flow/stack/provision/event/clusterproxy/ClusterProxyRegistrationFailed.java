package com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy;

import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class ClusterProxyRegistrationFailed extends StackFailureEvent {
    public ClusterProxyRegistrationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }
}
