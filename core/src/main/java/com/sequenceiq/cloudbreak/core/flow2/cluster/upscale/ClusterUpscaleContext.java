package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleContext extends CommonContext {

    private final String cloudPlatform;
    private final Stack stack;
    private final String hostGroupName;
    private final Integer scalingAdjustment;

    public ClusterUpscaleContext(String flowId, String cloudPlatform, Stack stack, String hostGroupName, Integer scalingAdjustment) {
        super(flowId);
        this.cloudPlatform = cloudPlatform;
        this.stack = stack;
        this.hostGroupName = hostGroupName;
        this.scalingAdjustment = scalingAdjustment;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public Stack getStack() {
        return stack;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public Integer getScalingAdjustment() {
        return scalingAdjustment;
    }
}
