package com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ClusterProxyRegistrationSuccess extends StackEvent {
    public ClusterProxyRegistrationSuccess(Long stackId) {
        super(stackId);
    }
}
