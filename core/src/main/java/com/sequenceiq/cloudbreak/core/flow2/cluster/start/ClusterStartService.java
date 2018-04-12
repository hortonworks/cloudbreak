package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;

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

    @Inject
    private StackUtil stackUtil;

    public void startingCluster(StackView stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.CLUSTER_OPERATION, String.format("Starting the Ambari cluster. Ambari ip: %s",
                stackUtil.extractAmbariIp(stack)));
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_STARTING, Status.UPDATE_IN_PROGRESS.name(), stackUtil.extractAmbariIp(stack));
    }

    public void clusterStartFinished(StackView stack) {
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        String ambariIp = stackUtil.extractAmbariIp(stack);
        cluster.setUpSince(new Date().getTime());
        clusterService.updateCluster(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.AVAILABLE);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Ambari cluster started.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_STARTED, Status.AVAILABLE.name(), ambariIp);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStartSuccessEmail(cluster.getOwner(), cluster.getEmailTo(), ambariIp, cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.AVAILABLE.name());
        }
    }

    public void handleClusterStartFailure(StackView stackView, String errorReason) {
        ClusterView clusterView = stackView.getClusterView();
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.START_FAILED);
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "Cluster could not be started: " + errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_START_FAILED, Status.START_FAILED.name(), errorReason);
        if (clusterView.getEmailNeeded()) {
            emailSenderService.sendStartFailureEmail(stackView.getClusterView().getOwner(), clusterView.getEmailTo(),
                    stackUtil.extractAmbariIp(stackView), clusterView.getName());
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.START_FAILED.name());
        }
    }
}
