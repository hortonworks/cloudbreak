package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("ClusterTerminationFinishedAction")
public class ClusterTerminationFinishedAction extends AbstractClusterTerminationAction<TerminateClusterResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFinishedAction.class);
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

    @Override
    protected Long getClusterId(TerminateClusterResult payload) {
        return payload.getRequest().getClusterContext().getClusterId();
    }

}
