package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterStartStopContext extends CommonContext {
    private final Stack stack;

    public ClusterStartStopContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
