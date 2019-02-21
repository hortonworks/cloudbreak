package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class EphemeralClusterService {

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void updateClusterStarted(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Ephemeral cluster update started");
        flowMessageService.fireEventAndLog(stackId, Msg.STACK_DATALAKE_UPDATE, UPDATE_IN_PROGRESS.name());
    }

    public void updateClusterFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Ephemeral cluster has been updated");
        flowMessageService.fireEventAndLog(stackId, Msg.STACK_DATALAKE_UPDATE_FINISHED, AVAILABLE.name());
    }

    public void updateClusterFailed(long stackId, Exception exception) {
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Ephemeral cluster update failed " + exception.getMessage());
        flowMessageService.fireEventAndLog(stackId, Msg.STACK_DATALAKE_UPDATE_FAILED, UPDATE_FAILED.name());
    }
}
