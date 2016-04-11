package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component("ClusterTerminationFinishedAction")
public class ClusterTerminationFinishedAction extends AbstractClusterTerminationAction<TerminateClusterResult> {
    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    protected ClusterTerminationFinishedAction() {
        super(TerminateClusterResult.class);
    }

    @Override
    protected void doExecute(ClusterContext context, TerminateClusterResult payload, Map<Object, Object> variables) throws Exception {
        clusterTerminationFlowService.finishClusterTermination(context, payload);
        sendEvent(context.getFlowId(), ClusterTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), null);
    }

}
