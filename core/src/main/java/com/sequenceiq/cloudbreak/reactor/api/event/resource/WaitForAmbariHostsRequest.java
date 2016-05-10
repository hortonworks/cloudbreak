package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class WaitForAmbariHostsRequest extends AbstractClusterUpscaleRequest {
    public WaitForAmbariHostsRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
