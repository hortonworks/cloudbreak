package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyRegistrationRequest extends StackEvent {
    public ClusterProxyRegistrationRequest(Long stackId) {
        super(stackId);
    }
}
