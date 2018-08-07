package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import java.time.Duration;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterStopService {
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

    public void stoppingCluster(long stackId) {
        updateClusterUptime(stackId);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_STOPPING, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stackId, Status.STOP_IN_PROGRESS);
    }

    private void updateClusterUptime(long stackId) {
        Cluster cluster = clusterService.retrieveClusterByStackId(stackId);
        cluster.setUptime(Duration.ofMillis(stackUtil.getUptimeForCluster(cluster, true)).toString());
        clusterService.updateCluster(cluster);
    }

    public void clusterStopFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, Status.STOPPED);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_STOPPED, Status.STOPPED.name());
    }

    public void handleClusterStopFailure(StackView stackView, String errorReason) {
        ClusterView cluster = stackView.getClusterView();
        clusterService.updateClusterStatusByStackId(stackView.getId(), Status.STOP_FAILED);
        stackUpdater.updateStackStatus(stackView.getId(), DetailedStackStatus.AVAILABLE, "The Ambari cluster could not be stopped: " + errorReason);
        flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_STOP_FAILED, Status.STOP_FAILED.name(), errorReason);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStopFailureEmail(stackView.getClusterView().getOwner(), stackView.getClusterView().getEmailTo(),
                    stackUtil.extractAmbariIp(stackView), cluster.getName());
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.STOP_FAILED.name());
        }
    }
}
