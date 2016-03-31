package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

abstract class AbstractClusterTerminationAction<P> extends AbstractAction<ClusterTerminationState, ClusterTerminationEvent, ClusterContext, P> {

    @Inject
    private ClusterService clusterService;

    protected AbstractClusterTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterContext createFlowContext(StateContext<ClusterTerminationState, ClusterTerminationEvent> stateContext, P payload) {
        String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
        Cluster cluster = clusterService.getById(getClusterId(payload));
        return new ClusterContext(flowId, cluster);
    }

    @Override
    protected Object getFailurePayload(ClusterContext flowContext, Exception ex) {
        return null;
    }

    @Override
    protected Selectable createRequest(ClusterContext context) {
        return null;
    }

    protected abstract Long getClusterId(P payload);
}
