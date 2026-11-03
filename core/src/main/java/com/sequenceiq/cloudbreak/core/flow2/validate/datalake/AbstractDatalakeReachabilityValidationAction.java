package com.sequenceiq.cloudbreak.core.flow2.validate.datalake;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.datalake.config.DatalakeReachabilityValidationState;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractDatalakeReachabilityValidationAction<P extends Payload>
        extends AbstractStackAction<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent, CommonContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractDatalakeReachabilityValidationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected CommonContext createFlowContext(FlowParameters flowParameters,
            StateContext<DatalakeReachabilityValidationState, DatalakeReachabilityValidationEvent> stateContext, P payload) {
        MDCBuilder.buildMdcContext(stackDtoService.getStackViewById(payload.getResourceId()));
        return new CommonContext(flowParameters);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
