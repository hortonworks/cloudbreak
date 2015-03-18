package com.sequenceiq.cloudbreak.core.flow.context;


import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class UpdateInstancesFlowContext implements FlowContext {

    private CloudPlatform cloudPlatform;
    private Long stackId;
    private Integer scalingAdjustment;
    private String instanceGroup;

    public UpdateInstancesFlowContext() { }

    public UpdateInstancesFlowContext(UpdateInstancesRequest updateInstancesRequest) {
        this.cloudPlatform = updateInstancesRequest.getCloudPlatform();
        this.stackId = updateInstancesRequest.getStackId();
        this.scalingAdjustment = updateInstancesRequest.getScalingAdjustment();
        this.instanceGroup = updateInstancesRequest.getInstanceGroup();
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }
}
