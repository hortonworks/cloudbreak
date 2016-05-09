package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowRegister;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackFailureAction<S extends FlowState, E extends FlowEvent> extends AbstractAction<S, E, StackFailureContext, FlowFailureEvent> {
    @Inject
    private StackService stackService;
    @Inject
    private FlowRegister runningFlows;

    protected AbstractStackFailureAction() {
        super(FlowFailureEvent.class);
    }

    @Override
    protected StackFailureContext createFlowContext(String flowId, StateContext<S, E> stateContext, FlowFailureEvent payload) {
        Flow flow = runningFlows.get(flowId);
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        flow.setFlowFailed();
        return new StackFailureContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(FlowFailureEvent payload, Optional<StackFailureContext> flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<FlowFailureEvent>> payloadConverters) {
        payloadConverters.add(new CloudPlatformResponseToFlowFailureConverter());
    }
}
