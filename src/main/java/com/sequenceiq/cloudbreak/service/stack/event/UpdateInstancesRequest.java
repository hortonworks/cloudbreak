package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class UpdateInstancesRequest extends ProvisionEvent {

    private Integer scalingAdjustment;
    private String instanceGroup;

    public UpdateInstancesRequest(CloudPlatform cloudPlatform, Long stackId, Integer scalingAdjustment, String instanceGroup) {
        super(cloudPlatform, stackId);
        this.scalingAdjustment = scalingAdjustment;
        this.instanceGroup = instanceGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }
}
