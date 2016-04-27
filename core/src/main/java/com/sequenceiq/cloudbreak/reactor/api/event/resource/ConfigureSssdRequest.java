package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ConfigureSssdRequest extends AbstractClusterUpscaleRequest {

    public ConfigureSssdRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
