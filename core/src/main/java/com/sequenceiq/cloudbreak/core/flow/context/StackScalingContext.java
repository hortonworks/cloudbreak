package com.sequenceiq.cloudbreak.core.flow.context;


import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class StackScalingContext extends DefaultFlowContext {

    private final Integer scalingAdjustment;
    private final ScalingType scalingType;
    private String instanceGroup;
    private Set<Resource> resources;
    private Set<String> upscaleCandidateAddresses;

    public StackScalingContext(Long stackId, Platform platform, Integer scalingAdjustment, String instanceGroup, Set<Resource> resources,
            ScalingType scalingType, Set<String> upscaleCandidateAddresses) {
        super(stackId, platform);
        this.scalingAdjustment = scalingAdjustment;
        this.instanceGroup = instanceGroup;
        this.resources = resources;
        this.scalingType = scalingType;
        this.upscaleCandidateAddresses = upscaleCandidateAddresses;
    }

    public StackScalingContext(Long stackId, Platform platform, Integer scalingAdjustment, String instanceGroup, ScalingType scalingType) {
        super(stackId, platform);
        this.scalingAdjustment = scalingAdjustment;
        this.instanceGroup = instanceGroup;
        this.scalingType = scalingType;
    }

    public StackScalingContext(UpdateInstancesRequest updateInstancesRequest) {
        super(updateInstancesRequest.getStackId(), updateInstancesRequest.getCloudPlatform());
        this.scalingAdjustment = updateInstancesRequest.getScalingAdjustment();
        this.instanceGroup = updateInstancesRequest.getInstanceGroup();
        this.scalingType = updateInstancesRequest.getScalingType();
        this.resources = null;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
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

    public Set<Resource> getResources() {
        return resources;
    }

    public Set<String> getUpscaleCandidateAddresses() {
        return upscaleCandidateAddresses;
    }
}
