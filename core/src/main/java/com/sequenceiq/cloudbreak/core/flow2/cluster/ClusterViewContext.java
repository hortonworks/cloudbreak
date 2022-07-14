package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterViewContext extends CommonContext {

    private final StackView stack;

    private final ClusterView cluster;

    public ClusterViewContext(FlowParameters flowParameters, StackView stack, ClusterView cluster) {
        super(flowParameters);
        this.stack = stack;
        this.cluster = cluster;
    }

    public StackView getStack() {
        return stack;
    }

    public long getStackId() {
        return stack.getId();
    }

    public long getClusterId() {
        return getCluster().getId();
    }

    public ClusterView getCluster() {
        return cluster;
    }
}
