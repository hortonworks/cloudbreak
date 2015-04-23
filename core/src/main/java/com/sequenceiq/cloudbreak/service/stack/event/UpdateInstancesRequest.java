package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.ScalingType;

public class UpdateInstancesRequest extends ProvisionEvent {

    private Integer scalingAdjustment;
    private String instanceGroup;
    private ScalingType scalingType;

    public UpdateInstancesRequest(CloudPlatform cloudPlatform, Long stackId, Integer scalingAdjustment, String instanceGroup, ScalingType scalingType) {
        super(cloudPlatform, stackId);
        this.scalingAdjustment = scalingAdjustment;
        this.instanceGroup = instanceGroup;
        this.scalingType = scalingType;
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

    public ScalingType getScalingType() {
        return scalingType;
    }
}
