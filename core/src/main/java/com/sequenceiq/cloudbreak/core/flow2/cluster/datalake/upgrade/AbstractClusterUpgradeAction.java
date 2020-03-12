package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterUpgradeAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, ClusterUpgradeContext, P> {

    protected AbstractClusterUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }
}
