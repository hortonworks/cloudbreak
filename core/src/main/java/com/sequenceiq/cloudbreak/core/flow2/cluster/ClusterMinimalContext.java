package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.ClusterMinimal;
import com.sequenceiq.cloudbreak.domain.StackMinimal;

public class ClusterMinimalContext extends CommonContext {

    private final StackMinimal stack;

    public ClusterMinimalContext(String flowId, StackMinimal stack) {
        super(flowId);
        this.stack = stack;
    }

    public StackMinimal getStack() {
        return stack;
    }

    public ClusterMinimal getCluster() {
        return stack.getCluster();
    }

    public long getStackId() {
        return stack.getId();
    }

    public long getClusterId() {
        return getCluster().getId();
    }
}
