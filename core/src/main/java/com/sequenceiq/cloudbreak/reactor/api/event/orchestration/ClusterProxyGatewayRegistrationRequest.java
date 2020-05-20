package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyGatewayRegistrationRequest extends StackEvent {

    private String cloudPlatform;

    public ClusterProxyGatewayRegistrationRequest(Long stackId, String cloudPlatform) {
        super(stackId);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
