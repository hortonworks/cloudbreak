package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterUpgradeValidationContext extends ClusterUpgradeContext {

    public ClusterUpgradeValidationContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters, event);
    }
}
