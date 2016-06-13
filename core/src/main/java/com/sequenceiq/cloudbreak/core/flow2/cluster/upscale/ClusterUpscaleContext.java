package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleContext extends ClusterContext {
    private final String hostGroupName;
    private final Integer adjustment;

    public ClusterUpscaleContext(String flowId, Stack stack, String hostGroupName, Integer adjustment) {
        super(flowId, stack, stack.getCluster());
        this.hostGroupName = hostGroupName;
        this.adjustment = adjustment;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
