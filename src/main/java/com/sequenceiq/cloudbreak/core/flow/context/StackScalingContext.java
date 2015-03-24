package com.sequenceiq.cloudbreak.core.flow.context;


import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class StackScalingContext extends DefaultFlowContext implements FlowContext {

    private Integer scalingAdjustment;
    private String instanceGroup;

    public StackScalingContext(Long stackId, CloudPlatform cloudPlatform, String instanceGroup, Integer scalingAdjustment) {
        super(stackId, cloudPlatform);
        this.instanceGroup = instanceGroup;
        this.scalingAdjustment = scalingAdjustment;
    }

    public StackScalingContext(UpdateInstancesRequest updateInstancesRequest) {
        super(updateInstancesRequest.getStackId(), updateInstancesRequest.getCloudPlatform());
        this.scalingAdjustment = updateInstancesRequest.getScalingAdjustment();
        this.instanceGroup = updateInstancesRequest.getInstanceGroup();
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
