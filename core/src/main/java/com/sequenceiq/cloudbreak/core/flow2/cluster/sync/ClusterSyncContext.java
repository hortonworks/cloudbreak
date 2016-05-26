package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterSyncContext extends CommonContext {
    private final Stack stack;

    public ClusterSyncContext(String flowId, Stack stack) {
        super(flowId);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}
