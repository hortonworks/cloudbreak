package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterContext extends CommonContext {
    private Stack stack;
    private Cluster cluster;

    public ClusterContext(String flowId, Stack stack, Cluster cluster) {
        super(flowId);
        this.stack = stack;
        this.cluster = cluster;
    }

    public Stack getStack() {
        return stack;
    }

    public Cluster getCluster() {
        return cluster;
    }
}
