package com.sequenceiq.cloudbreak.core.flow2.cluster.stop;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
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
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_STOPPING, Status.UPDATE_IN_PROGRESS.name());
        clusterService.updateClusterStatusByStackId(stackId, Status.STOP_IN_PROGRESS);
    }

    public void clusterStopFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, Status.STOPPED);
        flowMessageService.fireEventAndLog(stackId, Msg.AMBARI_CLUSTER_STOPPED, Status.STOPPED.name());
    }

    public void handleClusterStopFailure(StackView stack, String errorReason) {
        ClusterView cluster = stack.getCluster();
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.STOP_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "The Ambari cluster could not be stopped: " + errorReason);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_STOP_FAILED, Status.AVAILABLE.name(), errorReason);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStopFailureEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stackUtil.extractAmbariIp(stack), cluster.getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, Status.STOP_FAILED.name());
        }
    }
}
