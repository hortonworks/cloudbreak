package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleContext extends CommonContext {

    private final Stack stack;
    private final String hostGroupName;

    public ClusterUpscaleContext(String flowId, Stack stack, String hostGroupName) {
        super(flowId);
        this.stack = stack;
        this.hostGroupName = hostGroupName;
    }

    public Stack getStack() {
        return stack;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
