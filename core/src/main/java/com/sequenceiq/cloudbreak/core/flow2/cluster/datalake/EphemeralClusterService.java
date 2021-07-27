package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DATALAKE_UPDATE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DATALAKE_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DATALAKE_UPDATE_FINISHED;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
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
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), STACK_DATALAKE_UPDATE);
    }

    public void updateClusterFinished(long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Ephemeral cluster has been updated");
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), STACK_DATALAKE_UPDATE_FINISHED);
    }

    public void updateClusterFailed(long stackId, Exception exception) {
        String errorMessage = "Ephemeral cluster update failed " + exception.getMessage();
        clusterService.updateClusterStatusByStackId(stackId, AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, errorMessage);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), STACK_DATALAKE_UPDATE_FAILED, errorMessage);
    }
}
