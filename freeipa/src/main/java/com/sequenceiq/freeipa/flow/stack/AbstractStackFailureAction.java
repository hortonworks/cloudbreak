package com.sequenceiq.freeipa.flow.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractStackFailureAction<S extends FlowState, E extends FlowEvent>
        extends AbstractStackAction<S, E, StackFailureContext, StackFailureEvent> {
    @Inject
    private StackService stackService;

    protected AbstractStackFailureAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected StackFailureContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, StackFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        Stack stack = stackService.getStackById(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        flow.setFlowFailed(payload.getException());
        return new StackFailureContext(flowParameters, stack);
    }

    @Override
    protected Object getFailurePayload(StackFailureEvent payload, Optional<StackFailureContext> flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<StackFailureEvent>> payloadConverters) {
        payloadConverters.add(new CloudPlatformResponseToStackFailureConverter());
    }
}
