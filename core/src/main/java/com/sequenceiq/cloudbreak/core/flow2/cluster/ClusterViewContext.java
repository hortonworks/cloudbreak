package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterViewContext extends CommonContext {

    private final StackView stack;

    public ClusterViewContext(FlowParameters flowParameters, StackView stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }

    public ClusterView getClusterView() {
        return stack.getClusterView();
    }

    public long getStackId() {
        return stack.getId();
    }

    public long getClusterId() {
        return getClusterView().getId();
    }
}
