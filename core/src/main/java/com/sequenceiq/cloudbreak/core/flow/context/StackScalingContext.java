package com.sequenceiq.cloudbreak.core.flow.context;


import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.ScalingType;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class StackScalingContext extends DefaultFlowContext implements FlowContext {

    private Integer scalingAdjustment;
    private String instanceGroup;
    private ScalingType scalingType;

    public StackScalingContext(Long stackId, CloudPlatform cloudPlatform, String instanceGroup, Integer scalingAdjustment, ScalingType scalingType) {
        super(stackId, cloudPlatform);
        this.instanceGroup = instanceGroup;
        this.scalingAdjustment = scalingAdjustment;
        this.scalingType = scalingType;
    }

    public StackScalingContext(UpdateInstancesRequest updateInstancesRequest) {
        super(updateInstancesRequest.getStackId(), updateInstancesRequest.getCloudPlatform());
        this.scalingAdjustment = updateInstancesRequest.getScalingAdjustment();
        this.instanceGroup = updateInstancesRequest.getInstanceGroup();
        this.scalingType = updateInstancesRequest.getScalingType();
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
