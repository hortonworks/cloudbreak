package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AddNodeRequest extends ProvisionEvent {

    private Integer scalingAdjustment;

    public AddNodeRequest(CloudPlatform cloudPlatform, Long stackId, Integer scalingAdjustment) {
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
