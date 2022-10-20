package com.sequenceiq.cloudbreak.core.flow2.cluster.sync;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterSyncContext extends CommonContext {
    private final StackView stack;

    public ClusterSyncContext(FlowParameters flowParameters, StackView stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }
}
