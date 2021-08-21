package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.BackupRestoreEvent;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractBackupRestoreActions<P extends BackupRestoreEvent>
    extends AbstractAction<FlowState, FlowEvent, BackupRestoreContext, P> {

    protected AbstractBackupRestoreActions(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected BackupRestoreContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
            P payload) {
        return BackupRestoreContext.from(flowParameters, payload, payload.getBackupLocation(), payload.getBackupId(), payload.getCloseConnections());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<BackupRestoreContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}