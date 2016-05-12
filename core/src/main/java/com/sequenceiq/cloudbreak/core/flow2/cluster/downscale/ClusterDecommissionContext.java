package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterScaleContext;
import com.sequenceiq.cloudbreak.domain.Stack;

class ClusterDecommissionContext extends ClusterScaleContext {

    private final Integer scalingAdjustment;

    ClusterDecommissionContext(String flowId, Stack stack, String hostGroupName, Integer scalingAdjustment) {
        super(flowId, stack, hostGroupName);
        this.scalingAdjustment = scalingAdjustment;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
