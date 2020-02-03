package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.action;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractExternalDatabaseCreationAction<P extends Payload>
        extends AbstractStackAction<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent, ExternalDatabaseContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractExternalDatabaseCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ExternalDatabaseContext createFlowContext(FlowParameters flowParameters,
            StateContext<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent> stateContext, P payload) {

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
            StateContext<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent> stateContext, P payload) {
    }
}
