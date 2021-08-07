package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterCreationViewContext extends ClusterViewContext {

    private final ProvisionType provisionType;

    public ClusterCreationViewContext(FlowParameters flowParameters, StackView stack, ProvisionType provisionType) {
        super(flowParameters, stack);
        this.provisionType = provisionType;
    }

    public ProvisionType getProvisionType() {
        return provisionType;
    }
}
