package com.sequenceiq.cloudbreak.core.flow2.stack;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackFailureAction<S extends FlowState, E extends FlowEvent>
        extends AbstractStackAction<S, E, StackFailureContext, StackFailureEvent> {
    @Inject
    private StackService stackService;

    protected AbstractStackFailureAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected StackFailureContext createFlowContext(String flowId, StateContext<S, E> stateContext, StackFailureEvent payload) {
        Flow flow = getFlow(flowId);
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        flow.setFlowFailed(payload.getException());
        return new StackFailureContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(StackFailureEvent payload, Optional<StackFailureContext> flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void initPayloadConverterMap(List<PayloadConverter<StackFailureEvent>> payloadConverters) {
        payloadConverters.add(new CloudPlatformResponseToStackFailureConverter());
        payloadConverters.add(new ClusterPlatformResponseToStackFailureConverter());
    }
}
