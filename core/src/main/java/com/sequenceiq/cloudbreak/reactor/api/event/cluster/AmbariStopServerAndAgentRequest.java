package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariStopServerAndAgentRequest extends AbstractClusterScaleRequest {
    public AmbariStopServerAndAgentRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
