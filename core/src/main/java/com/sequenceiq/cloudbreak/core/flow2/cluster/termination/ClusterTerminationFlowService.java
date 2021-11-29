package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_STARTED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class ClusterTerminationFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFlowService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void terminateCluster(ClusterViewContext context) {
        clusterService.updateClusterStatusByStackId(context.getStackId(), DetailedStackStatus.CLUSTER_DELETE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(context.getStackId(), Status.DELETE_IN_PROGRESS.name(), CLUSTER_DELETE_STARTED);
        LOGGER.debug("Cluster delete started.");
    }

    public void finishClusterTerminationAllowed(ClusterViewContext context, ClusterTerminationResult payload) {
        LOGGER.debug("Terminate cluster result: {}", payload);
        StackView stackView = context.getStack();
        ClusterView clusterView = context.getClusterView();
        if (clusterView != null) {
            InMemoryStateStore.deleteCluster(clusterView.getId());
            stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.CLUSTER_DELETE_COMPLETED);
        }
    }

    public void finishClusterTerminationNotAllowed(ClusterViewContext context, ClusterTerminationResult payload) {
        StackView stackView = context.getStack();
        Long stackId = stackView.getId();
        flowMessageService.fireEventAndLog(stackId, DELETE_FAILED.name(), CLUSTER_DELETE_FAILED, "Operation not allowed");
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
    }

    public void handleClusterTerminationError(StackFailureEvent payload) {
        LOGGER.debug("Handling cluster delete failure event.");
        Exception errorDetails = payload.getException();
        LOGGER.info("Error during cluster termination flow: ", errorDetails);
        stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.CLUSTER_DELETE_FAILED, errorDetails.getMessage());
        flowMessageService.fireEventAndLog(payload.getResourceId(), DELETE_FAILED.name(), CLUSTER_DELETE_FAILED, errorDetails.getMessage());
    }

}
