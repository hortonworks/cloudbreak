package com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ClusterProxyRegistrationRequest extends StackEvent {
    public ClusterProxyRegistrationRequest(Long stackId) {
        super(stackId);
    }
}
