package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import java.util.Optional;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoverySetupNewInstancesFailedEvent;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractDatalakeRecoveryBringupAction<P extends StackEvent>
        extends AbstractAction<FlowState, FlowEvent, DatalakeRecoveryBringupContext, P> {

    protected AbstractDatalakeRecoveryBringupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected DatalakeRecoveryBringupContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, P payload) {
        return DatalakeRecoveryBringupContext.from(flowParameters, payload);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<DatalakeRecoveryBringupContext> flowContext, Exception ex) {
        return DatalakeRecoverySetupNewInstancesFailedEvent.from(payload, ex, DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
    }

}
