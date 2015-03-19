package com.sequenceiq.cloudbreak.core.flow.context;


import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class StackScalingContext implements FlowContext {

    private CloudPlatform cloudPlatform;
    private Long stackId;
    private Integer scalingAdjustment;
    private String instanceGroup;
    private String errorMessage = "";

    public StackScalingContext() { }

    public StackScalingContext(UpdateInstancesRequest updateInstancesRequest) {
        this.cloudPlatform = updateInstancesRequest.getCloudPlatform();
        this.stackId = updateInstancesRequest.getStackId();
        this.scalingAdjustment = updateInstancesRequest.getScalingAdjustment();
        this.instanceGroup = updateInstancesRequest.getInstanceGroup();
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
