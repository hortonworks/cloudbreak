package com.sequenceiq.cloudbreak.core.flow2.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.domain.ClusterMinimal;
import com.sequenceiq.cloudbreak.domain.StackMinimal;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractClusterAction<P extends Payload> extends AbstractAction<FlowState, FlowEvent, ClusterMinimalContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractClusterAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterMinimalContext createFlowContext(String flowId, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        StackMinimal stack = stackService.getMinimalById(payload.getStackId());
        ClusterMinimal cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(stack.getId().toString(), stack.getName(), stack.getOwner(), "CLUSTER");
        return new ClusterMinimalContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterMinimalContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }

    public StackService getStackService() {
        return stackService;
    }
}
