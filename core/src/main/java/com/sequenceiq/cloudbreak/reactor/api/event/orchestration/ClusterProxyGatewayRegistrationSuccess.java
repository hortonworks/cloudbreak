package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyGatewayRegistrationSuccess extends StackEvent {
    public ClusterProxyGatewayRegistrationSuccess(Long stackId) {
        super(stackId);
    }
}
