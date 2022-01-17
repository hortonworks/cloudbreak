package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class ClusterProxyReRegistrationRequest extends AbstractClusterScaleRequest {

    private String cloudPlatform;

    public ClusterProxyReRegistrationRequest(Long stackId, Set<String> hostGroups, String cloudPlatform) {
        super(stackId, hostGroups);
        this.cloudPlatform = cloudPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }
}
