package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Service
public class ClusterTerminationFlowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationFlowService.class);

    @Inject
    private ClusterTerminationService terminationService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    public void terminateCluster(ClusterContext context) {
        clusterService.updateClusterStatusByStackId(context.getStack().getId(), Status.DELETE_IN_PROGRESS);
        LOGGER.info("Cluster delete started.");
    }

    public void finishClusterTermination(ClusterContext context, ClusterTerminationResult payload) {
        LOGGER.info("Terminate cluster result: {}", payload);
        Cluster cluster = context.getCluster();
        terminationService.finalizeClusterTermination(cluster.getId());
        flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_DELETE_COMPLETED, DELETE_COMPLETED.name(), cluster.getId());
        clusterService.updateClusterStatusByStackId(cluster.getStack().getId(), DELETE_COMPLETED);
        InMemoryStateStore.deleteCluster(cluster.getId());
        stackUpdater.updateStackStatus(cluster.getStack().getId(), AVAILABLE);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendTerminationSuccessEmail(cluster.getOwner(), cluster.getEmailTo(), cluster.getAmbariIp(), cluster.getName());
            flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_EMAIL_SENT, DELETE_COMPLETED.name());
        }
    }

    public void handleClusterTerminationError(StackFailureEvent payload) {
        LOGGER.info("Handling cluster delete failure event.");
        Exception errorDetails = payload.getException();
        LOGGER.error("Error during cluster termination flow: ", errorDetails);
        Cluster cluster = clusterService.retrieveClusterByStackId(payload.getStackId());
        cluster.setStatus(DELETE_FAILED);
        cluster.setStatusReason(errorDetails.getMessage());
        clusterService.updateCluster(cluster);
        flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_DELETE_FAILED, DELETE_FAILED.name(), errorDetails.getMessage());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendTerminationFailureEmail(cluster.getOwner(), cluster.getEmailTo(), cluster.getAmbariIp(), cluster.getName());
            flowMessageService.fireEventAndLog(cluster.getStack().getId(), Msg.CLUSTER_EMAIL_SENT, DELETE_FAILED.name());
        }
    }
}
