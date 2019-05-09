package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;

public class ClusterSyncContext extends CommonContext {
    private final StackView stack;

    public ClusterSyncContext(String flowId, StackView stack) {
        super(flowId);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }
}
