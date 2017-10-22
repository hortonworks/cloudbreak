package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.StackMinimal;

public class ClusterSyncContext extends CommonContext {
    private final StackMinimal stack;

    public ClusterSyncContext(String flowId, StackMinimal stack) {
        super(flowId);
        this.stack = stack;
    }

    public StackMinimal getStack() {
        return stack;
    }
}
