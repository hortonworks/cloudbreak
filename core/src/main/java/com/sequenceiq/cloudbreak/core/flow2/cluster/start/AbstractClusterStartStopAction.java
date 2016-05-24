package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractClusterStartStopAction<P extends Payload>
        extends AbstractAction<ClusterStartState, ClusterStartEvent, ClusterStartStopContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractClusterStartStopAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterStartStopContext createFlowContext(String flowId, StateContext<ClusterStartState, ClusterStartEvent> stateContext, P payload) {
        Stack stack = stackService.getById(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        return new ClusterStartStopContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterStartStopContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
