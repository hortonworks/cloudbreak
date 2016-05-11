package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.domain.Stack;

public class AddClusterContainersContext extends ClusterUpscaleContext {
    private final Integer scalingAdjustment;

    public AddClusterContainersContext(String flowId, Stack stack, String hostGroupName, Integer scalingAdjustment) {
        super(flowId, stack, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
