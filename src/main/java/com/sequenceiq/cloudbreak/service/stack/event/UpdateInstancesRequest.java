package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class UpdateInstancesRequest extends ProvisionEvent {

    private Integer scalingAdjustment;

    public UpdateInstancesRequest(CloudPlatform cloudPlatform, Long stackId, Integer scalingAdjustment) {
        super(cloudPlatform, stackId);
        this.scalingAdjustment = scalingAdjustment;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }
}
