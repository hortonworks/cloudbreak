package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow.context.AmbariClusterContext;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterRequest;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component("ClusterTerminationAction")
public class ClusterTerminationAction extends AbstractClusterTerminationAction<DefaultClusterFlowContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationAction.class);

    @Inject
    private ClusterService clusterService;

    protected ClusterTerminationAction() {
        super(DefaultClusterFlowContext.class);
    }

    @Override
    protected void doExecute(ClusterContext context, DefaultClusterFlowContext payload, Map<Object, Object> variables) throws Exception {
        AmbariClusterContext clusterContext = new AmbariClusterContext(
                context.getCluster().getStack().getId(),
                context.getCluster().getStack().getName(),
                context.getCluster().getId(),
                context.getCluster().getOwner());
        // TODO: check if context is ok and send an event
        TerminateClusterRequest terminateRequest = new TerminateClusterRequest(clusterContext);
        clusterService.updateClusterStatusByStackId(context.getCluster().getStack().getId(), Status.DELETE_IN_PROGRESS);
        LOGGER.info("Cluster delete started.");
        sendEvent(context.getFlowId(), AmbariClusterRequest.selector(TerminateClusterRequest.class), terminateRequest);
    }

    @Override
    protected Long getClusterId(DefaultClusterFlowContext payload) {
        return payload.getClusterId();
    }
}
