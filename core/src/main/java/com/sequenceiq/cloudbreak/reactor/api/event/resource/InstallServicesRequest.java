package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class InstallServicesRequest extends AbstractClusterUpscaleRequest {

    public InstallServicesRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
