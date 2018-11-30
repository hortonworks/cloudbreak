package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariStartServerAndAgentRequest extends AbstractClusterScaleRequest {
    public AmbariStartServerAndAgentRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
