package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_NOT_NEEDED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED_NOVERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_NOT_NEEDED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Service
public class ClusterUpgradeService {

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    public boolean upgradeClusterManager(long stackId, StatedImage currentImage, StatedImage targetImage) {
        String currentCmBuildNumber = currentImage.getImage().getCmBuildNumber();
        boolean clusterManagerUpdateNeeded = isUpdateNeeded(currentCmBuildNumber, targetImage.getImage().getCmBuildNumber());
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_UPGRADE, targetImage.getImage().getUuid());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        if (clusterManagerUpdateNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE);
        } else {
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_NOT_NEEDED, currentCmBuildNumber);
        }
        return clusterManagerUpdateNeeded;
    }

    public boolean upgradeCluster(long stackId, StatedImage currentImage, StatedImage targetImage) {
        Image targetIm = targetImage.getImage();
        Image currentIm = currentImage.getImage();
        String currentRuntimeBuildNumber = NullUtil.getIfNotNull(currentIm.getStackDetails(), StackDetails::getStackBuildNumber);
        boolean clusterManagerUpdateNeeded = isUpdateNeeded(currentIm.getCmBuildNumber(), targetIm.getCmBuildNumber());
        boolean clusterRuntimeUpgradeNeeded =
                isUpdateNeeded(currentRuntimeBuildNumber, NullUtil.getIfNotNull(targetIm.getStackDetails(), StackDetails::getStackBuildNumber));

        if (clusterManagerUpdateNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_FINISHED, targetIm.getVersion());
        }
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        if (clusterRuntimeUpgradeNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        } else {
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE_NOT_NEEDED, currentRuntimeBuildNumber);
        }
        return clusterRuntimeUpgradeNeeded;
    }

    public void clusterUpgradeFinished(long stackId, StatedImage currentImage, StatedImage targetImage) {
        Image targetIm = targetImage.getImage();
        Image currentIm = currentImage.getImage();
        String clusterStackVersion = NullUtil.getIfNotNull(targetIm.getStackDetails(), StackDetails::getVersion);
        String currentRuntimeBuildNumber = NullUtil.getIfNotNull(currentIm.getStackDetails(), StackDetails::getStackBuildNumber);
        boolean clusterRuntimeUpgradeNeeded =
                isUpdateNeeded(currentRuntimeBuildNumber, NullUtil.getIfNotNull(targetIm.getStackDetails(), StackDetails::getStackBuildNumber));

        clusterService.updateClusterStatusByStackId(stackId, Status.AVAILABLE);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_UPGRADE_FINISHED, "Cluster stack was successfully upgraded.");
        if (clusterRuntimeUpgradeNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, clusterStackVersion);
        } else {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED_NOVERSION);
        }
    }

    public void handleUpgradeClusterFailure(long stackId, String errorReason, DetailedStackStatus detailedStatus) {
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_FAILED, errorReason);
        stackUpdater.updateStackStatus(stackId, detailedStatus);
        switch (detailedStatus) {
            case CLUSTER_MANAGER_UPGRADE_FAILED:
                flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), CLUSTER_MANAGER_UPGRADE_FAILED, errorReason);
                break;
            case CLUSTER_UPGRADE_FAILED:
            default:
                flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), CLUSTER_UPGRADE_FAILED, errorReason);
        }
    }

    private boolean isUpdateNeeded(String currentBuildNumber, String targetBuildNumber) {
        if (StringUtils.isEmpty(currentBuildNumber) || StringUtils.isEmpty(targetBuildNumber)) {
            return true;
        }
        return !currentBuildNumber.equals(targetBuildNumber);
    }

}
