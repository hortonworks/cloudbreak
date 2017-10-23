package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.ClusterView;
import com.sequenceiq.cloudbreak.domain.StackView;

public class ClusterViewContext extends CommonContext {

    private final StackView stack;

    public ClusterViewContext(String flowId, StackView stack) {
        super(flowId);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }

    public ClusterView getCluster() {
        return stack.getCluster();
    }

    public long getStackId() {
        return stack.getId();
    }

    public long getClusterId() {
        return getCluster().getId();
    }
}
