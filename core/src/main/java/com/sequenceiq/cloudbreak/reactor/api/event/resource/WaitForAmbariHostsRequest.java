package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class WaitForAmbariHostsRequest extends AbstractClusterScaleRequest {
    public WaitForAmbariHostsRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
