package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class AddClusterContainersRequest extends AbstractClusterUpscaleRequest {


    public AddClusterContainersRequest(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment) {
        super(stackId, cloudPlatform, hostGroupName, scalingAdjustment);
    }
}
