package com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterUpgradeService {
    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private StackUtil stackUtil;

    public void upgradeCluster(Stack stack, Cluster cluster) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_UPGRADE, Status.UPDATE_IN_PROGRESS.name());
    }

    public void clusterUpgradeFinished(Stack stack) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_REQUESTED);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE, "Ambari is successfully upgraded.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_UPGRADE_FINISHED, Status.AVAILABLE.name(), stackUtil.extractAmbariIp(stack));
    }

    public void handleUpgradeClusterFailure(Stack stack, String errorReason) {
        clusterService.updateClusterStatusByStackId(stack.getId(), Status.UPDATE_FAILED, errorReason);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_UPGRADE_FAILED, Status.UPDATE_FAILED.name(), errorReason);
    }
}
