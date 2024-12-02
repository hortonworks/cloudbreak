package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterTerminationContext extends ClusterViewContext {

    private TerminationType terminationType;

    public ClusterTerminationContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, TerminationType terminationType) {
        super(flowParameters, stack, cluster);
        this.terminationType = terminationType;
    }

    public boolean isForced() {
        return terminationType != null && terminationType.isForced();
    }

}
