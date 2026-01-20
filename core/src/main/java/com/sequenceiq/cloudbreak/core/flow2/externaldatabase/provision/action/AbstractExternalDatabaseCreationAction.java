package com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.action;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseContext;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.provision.config.ExternalDatabaseCreationState;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractExternalDatabaseCreationAction<P extends Payload>
        extends AbstractStackAction<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent, ExternalDatabaseContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractExternalDatabaseCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ExternalDatabaseContext createFlowContext(FlowParameters flowParameters,
            StateContext<ExternalDatabaseCreationState, ExternalDatabaseCreationEvent> stateContext, P payload) {

        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        return new ExternalDatabaseContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ExternalDatabaseContext> flowContext, Exception ex) {
        return payload;
    }
}
