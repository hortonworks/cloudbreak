package com.sequenceiq.cloudbreak.service.stack.event;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class UpdateInstancesRequest extends ProvisionEvent {

    private Integer scalingAdjustment;
    private String hostGroup;

    public UpdateInstancesRequest(CloudPlatform cloudPlatform, Long stackId, Integer scalingAdjustment, String hostGroup) {
        super(cloudPlatform, stackId);
        this.scalingAdjustment = scalingAdjustment;
        this.hostGroup = hostGroup;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(Integer scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }
}
