package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractClusterResetAction<P extends Payload> extends AbstractStackAction<ClusterResetState, ClusterResetEvent, ClusterViewContext, P> {
    @Inject
    private StackDtoService stackDtoService;

    protected AbstractClusterResetAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterViewContext createFlowContext(FlowParameters flowParameters, StateContext<ClusterResetState, ClusterResetEvent> stateContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        MDCBuilder.buildMdcContext(cluster);
        return new ClusterViewContext(flowParameters, stack, cluster);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterViewContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
