package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED_NOVERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_NOT_NEEDED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.util.NullUtil;

@Service
public class ClusterUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeService.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    public void initUpgradeCluster(long stackId, StatedImage targetImage) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), DATALAKE_UPGRADE, targetImage.getImage().getUuid());
        clusterService.updateClusterStatusByStackId(stackId, Status.UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_UPGRADE_STARTED, "Cluster upgrade has been started.");
    }

    public void upgradeClusterManager(long stackId) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE);
    }

    public boolean upgradeCluster(long stackId, Image currentImage, Image targetImage) {
        String currentRuntimeBuildNumber = NullUtil.getIfNotNull(currentImage.getStackDetails(), StackDetails::getStackBuildNumber);
        boolean clusterManagerUpdateNeeded = isUpdateNeeded(currentImage.getCmBuildNumber(), targetImage.getCmBuildNumber());
        boolean clusterRuntimeUpgradeNeeded =
                isUpdateNeeded(currentRuntimeBuildNumber, NullUtil.getIfNotNull(targetImage.getStackDetails(), StackDetails::getStackBuildNumber));

        if (clusterManagerUpdateNeeded) {
            String cmVersion = targetImage.getPackageVersion(ImagePackageVersion.CM);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE_FINISHED, cmVersion);
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

        Optional<String> stackVersion = getStackVersionFromImage(targetIm);
        stackVersion.ifPresentOrElse(s -> stackUpdater.updateStackVersion(stackId, s),
                () -> LOGGER.warn("Cluster runtime could not be upgraded for stack with id {}", stackId));
        if (clusterRuntimeUpgradeNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, clusterStackVersion);
        } else {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED_NOVERSION);
        }
    }

    private Optional<String> getStackVersionFromImage(Image image) {
        return Optional.ofNullable(image.getStackDetails())
                .map(StackDetails::getVersion);
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

    public boolean isClusterRuntimeUpgradeNeeded(Image currentImage, Image targetImage) {
        return isUpdateNeeded(NullUtil.getIfNotNull(currentImage.getStackDetails(), StackDetails::getStackBuildNumber),
                NullUtil.getIfNotNull(targetImage.getStackDetails(), StackDetails::getStackBuildNumber));
    }

    private boolean isUpdateNeeded(String currentBuildNumber, String targetBuildNumber) {
        if (StringUtils.isEmpty(currentBuildNumber) || StringUtils.isEmpty(targetBuildNumber)) {
            return true;
        }
        return !currentBuildNumber.equals(targetBuildNumber);
    }

}
