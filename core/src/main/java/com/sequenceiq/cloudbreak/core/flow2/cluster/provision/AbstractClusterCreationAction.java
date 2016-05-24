package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractClusterCreationAction<P extends Payload> extends AbstractAction<ClusterCreationState, ClusterCreationEvent, ClusterContext, P> {
    @Inject
    private StackService stackService;
    @Inject
    private ClusterService clusterService;

    protected AbstractClusterCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected ClusterContext createFlowContext(String flowId, StateContext<ClusterCreationState, ClusterCreationEvent> clusterContext, P payload) {
        Stack stack = stackService.getById(payload.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(payload.getStackId());
        // TODO LogAspect!!
        MDCBuilder.buildMdcContext(cluster);
        return new ClusterContext(flowId, stack, cluster);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
