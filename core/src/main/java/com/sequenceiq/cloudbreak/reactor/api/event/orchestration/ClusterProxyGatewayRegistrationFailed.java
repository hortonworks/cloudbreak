package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterProxyGatewayRegistrationFailed extends StackFailureEvent {
    public ClusterProxyGatewayRegistrationFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
