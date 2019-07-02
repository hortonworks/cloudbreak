package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyGatewayRegistrationRequest extends StackEvent {
    public ClusterProxyGatewayRegistrationRequest(Long stackId) {
        super(stackId);
    }
}
