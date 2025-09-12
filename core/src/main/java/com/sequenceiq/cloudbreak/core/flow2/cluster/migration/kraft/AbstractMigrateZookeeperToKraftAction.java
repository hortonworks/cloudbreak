package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractMigrateZookeeperToKraftAction<P extends Payload>
        extends AbstractAction<FlowState, FlowEvent, MigrateZookeeperToKraftContext, P> {

    protected AbstractMigrateZookeeperToKraftAction(Class<P> payloadClass) {
        super(payloadClass);
    }
}
