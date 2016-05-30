package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Service
public class ClusterStartService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartService.class);
    @Inject
    private ClusterService clusterService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private EmailSenderService emailSenderService;

    public void startingCluster(Stack stack, Cluster cluster) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, String.format("Starting the Ambari cluster. Ambari ip:%s",
                stack.getAmbariIp()));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_STARTING, Status.UPDATE_IN_PROGRESS.name(), stack.getAmbariIp());
    }

    public void clusterStartFinished(Stack stack) {
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        cluster.setUpSince(new Date().getTime());
        clusterService.updateCluster(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Ambari cluster started.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_STARTED, Status.AVAILABLE.name(), stack.getAmbariIp());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStartSuccessEmail(cluster.getOwner(), stack.getAmbariIp(), cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.AVAILABLE.name());
        }
    }

    public void handleClusterStartFailure(Stack stack, String errorReason) {
        Cluster cluster = stack.getCluster();
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Cluster could not be started: " + errorReason);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_START_FAILED, Status.AVAILABLE.name(), errorReason);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStartFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.START_FAILED.name());
        }
    }
}
