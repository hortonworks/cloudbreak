package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeContext;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterUpgradeValidationAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, ClusterUpgradeContext, P> {

    protected AbstractClusterUpgradeValidationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

}
