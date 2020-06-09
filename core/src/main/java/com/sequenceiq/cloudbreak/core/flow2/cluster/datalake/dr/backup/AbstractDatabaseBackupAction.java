package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractDatabaseBackupAction<P extends Payload>
    extends AbstractAction<FlowState, FlowEvent, DatabaseBackupContext, P> {

    protected AbstractDatabaseBackupAction(Class<P> payloadClass) {
        super(payloadClass);
    }
}