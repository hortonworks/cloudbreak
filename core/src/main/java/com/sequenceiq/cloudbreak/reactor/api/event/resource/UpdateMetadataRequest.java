package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateMetadataRequest extends AbstractClusterUpscaleRequest {

    public UpdateMetadataRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
