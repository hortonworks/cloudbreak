package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;

@Component("ClusterTerminationFailureAction")
public class ClusterTerminationFailureAction extends AbstractClusterTerminationAction<TerminateClusterResult> {
    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    protected ClusterTerminationFailureAction() {
        super(TerminateClusterResult.class);
    }

    @Override
    protected void doExecute(ClusterContext context, TerminateClusterResult payload, Map<Object, Object> variables) throws Exception {
        clusterTerminationFlowService.handleClusterTerminationError(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ClusterContext context) {
        return new SelectableEvent(ClusterTerminationEvent.CLUSTER_TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation());
    }

    @Override
    protected Long getClusterId(TerminateClusterResult payload) {
        return payload.getRequest().getClusterContext().getClusterId();
    }
}
