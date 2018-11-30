package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariRestartAllRequest extends AbstractClusterScaleRequest {
    public AmbariRestartAllRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
