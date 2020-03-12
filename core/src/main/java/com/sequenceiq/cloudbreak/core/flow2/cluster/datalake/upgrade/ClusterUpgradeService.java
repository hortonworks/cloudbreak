package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
public class ClusterUpgradeService {

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    public void upgradeClusterManager(long stackId, StatedImage targetImage) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_UPGRADE, targetImage.getImage().getUuid());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE);
    }

    public void upgradeCluster(long stackId, Image image) {
        String clusterManagerVersion = image.getVersion();

        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_MANAGER_UPGRADE_FINISHED, clusterManagerVersion);
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
    }

    public void clusterUpgradeFinished(long stackId, Image image) {
        String clusterStackVersion = image.getStackDetails().getVersion();

        clusterService.updateClusterStatusByStackId(stackId, Status.START_REQUESTED);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Cluster Manager is successfully upgraded.");
        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, clusterStackVersion);
    }

    public void handleUpgradeClusterFailure(long stackId, String errorReason) {
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_FAILED, errorReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), CLUSTER_UPGRADE_FAILED, errorReason);
    }
}
