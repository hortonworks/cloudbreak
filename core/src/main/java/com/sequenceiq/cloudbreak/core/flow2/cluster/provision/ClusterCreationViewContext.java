package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterCreationViewContext extends ClusterViewContext {

    private final ProvisionType provisionType;

    public ClusterCreationViewContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, ProvisionType provisionType) {
        super(flowParameters, stack, cluster);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
