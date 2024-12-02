package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractClusterTerminationAction<P extends Payload> extends AbstractStackAction<FlowState, FlowEvent, ClusterTerminationContext, P> {

    public static final String TERMINATION_TYPE = "TERMINATION_TYPE";

    @Inject
    private StackDtoService stackDtoService;

    protected AbstractClusterTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterTerminationContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        Map<Object, Object> variables = clusterContext.getExtendedState().getVariables();
        TerminationType terminationType = (TerminationType) variables.getOrDefault(TERMINATION_TYPE, TerminationType.REGULAR);
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContextFromInfoProvider(stack);
        MDCBuilder.buildMdcContextFromInfoProvider(cluster);
        return new ClusterTerminationContext(flowParameters, stack, cluster, terminationType);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterTerminationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
