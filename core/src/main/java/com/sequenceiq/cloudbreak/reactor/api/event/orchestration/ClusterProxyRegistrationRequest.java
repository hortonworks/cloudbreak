package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyRegistrationRequest extends StackEvent {

    private String cloudPlatform;

    public ClusterProxyRegistrationRequest(Long stackId, String cloudPlatform) {
        super(stackId);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
