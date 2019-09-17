package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class ClusterProxyReRegistrationRequest extends AbstractClusterScaleRequest {
    public ClusterProxyReRegistrationRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
