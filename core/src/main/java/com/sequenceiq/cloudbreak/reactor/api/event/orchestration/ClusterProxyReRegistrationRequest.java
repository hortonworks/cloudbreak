package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class ClusterProxyReRegistrationRequest extends AbstractClusterScaleRequest {

    private String cloudPlatform;

    public ClusterProxyReRegistrationRequest(Long stackId, String hostGroupName, String cloudPlatform) {
        super(stackId, hostGroupName);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
