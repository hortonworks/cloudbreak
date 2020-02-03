package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.action;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.terminate.config.ExternalDatabaseTerminationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractExternalDatabaseTerminationAction<P extends Payload>
        extends AbstractStackAction<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent, ExternalDatabaseContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractExternalDatabaseTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ExternalDatabaseContext createFlowContext(FlowParameters flowParameters,
            StateContext<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent> stateContext, P payload) {

        Stack stack = stackService.getByIdWithClusterInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        beforeReturnFlowContext(flowParameters, stateContext, payload);
        return new ExternalDatabaseContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ExternalDatabaseContext> flowContext, Exception ex) {
        return payload;
    }

    protected void beforeReturnFlowContext(FlowParameters flowParameters,
            StateContext<ExternalDatabaseTerminationState, ExternalDatabaseTerminationEvent> stateContext, P payload) {
    }
}
