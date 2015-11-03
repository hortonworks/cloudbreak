package com.sequenceiq.cloudbreak.core.flow.context;


import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateInstancesRequest;

public class StackScalingContext extends DefaultFlowContext {

    private Integer scalingAdjustment;
    private String instanceGroup;
    private Set<Resource> resources;
    private ScalingType scalingType;
    private Set<String> upscaleCandidateAddresses;

    public StackScalingContext(Long stackId, CloudPlatform cp, Integer sa, String ig, Set<Resource> resources, ScalingType st, Set<String> uca) {
        super(stackId, cp);
        this.scalingAdjustment = sa;
        this.instanceGroup = ig;
        this.resources = resources;
        this.scalingType = st;
        this.upscaleCandidateAddresses = uca;
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
