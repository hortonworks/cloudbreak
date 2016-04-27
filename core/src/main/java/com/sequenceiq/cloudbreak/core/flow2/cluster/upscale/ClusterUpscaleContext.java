package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterUpscaleContext extends CommonContext {

    private final Stack stack;

    ClusterUpscaleContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
