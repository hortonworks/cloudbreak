package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyRegistrationSuccess extends StackEvent {
    public ClusterProxyRegistrationSuccess(Long stackId) {
        super(stackId);
    }
}
