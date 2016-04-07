package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.context.AmbariClusterContext;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterRequest;

@Component("ClusterTerminationAction")
public class ClusterTerminationAction extends AbstractClusterTerminationAction<DefaultClusterFlowContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationAction.class);

    @Inject
    private ClusterTerminationFlowService clusterTerminationFlowService;

    protected ClusterTerminationAction() {
        super(DefaultClusterFlowContext.class);
    }

    @Override
    protected void doExecute(ClusterContext context, DefaultClusterFlowContext payload, Map<Object, Object> variables) throws Exception {
        clusterTerminationFlowService.terminateCluster(context);

        AmbariClusterContext clusterContext = new AmbariClusterContext(
                context.getCluster().getStack().getId(),
                context.getCluster().getStack().getName(),
                context.getCluster().getId(),
                context.getCluster().getOwner());
        // TODO: check if context is ok and send an event
        TerminateClusterRequest terminateRequest = new TerminateClusterRequest(clusterContext);
        sendEvent(context.getFlowId(), AmbariClusterRequest.selector(TerminateClusterRequest.class), terminateRequest);
    }

    @Override
    protected Long getClusterId(DefaultClusterFlowContext payload) {
        return payload.getClusterId();
    }
}
