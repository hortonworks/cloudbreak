package com.sequenceiq.cloudbreak.reactor.api.event.recipe;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyDeregisterRequest extends StackEvent {

    private String cloudPlatform;

    public ClusterProxyDeregisterRequest(Long stackId, String cloudPlatform) {
        super(stackId);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
