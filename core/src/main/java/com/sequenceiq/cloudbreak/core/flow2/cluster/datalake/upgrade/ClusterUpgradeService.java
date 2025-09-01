package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_MANAGER_ROLLING_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_ROLLING_UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_UPGRADE_STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FINISHED_NOVERSION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_ROLLING_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_UPGRADE;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
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

    public void initUpgradeCluster(long stackId, StatedImage targetImage, boolean rollingUpgradeEnabled) {
        String targetRuntime = targetImage.getImage().getStackDetails().getVersion();
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), rollingUpgradeEnabled ? DATALAKE_ROLLING_UPGRADE : DATALAKE_UPGRADE,
                targetRuntime, targetImage.getImage().getUuid());
        clusterService.updateClusterStatusByStackId(stackId, rollingUpgradeEnabled ? CLUSTER_ROLLING_UPGRADE_STARTED : CLUSTER_UPGRADE_STARTED,
                "Cluster upgrade has been started.");
    }

    public void upgradeClusterManager(long stackId) {
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_MANAGER_UPGRADE);
    }

    public void clusterUpgradeFinished(long stackId, Map<String, String> currentImagePackages, StatedImage targetImage, boolean rollingUpgradeEnabled) {
        Image targetIm = targetImage.getImage();
        String clusterStackVersion = NullUtil.getIfNotNull(targetIm.getStackDetails(), ImageStackDetails::getVersion);
        String currentRuntimeBuildNumber = getCurrentStackBuildNumber(currentImagePackages);
        boolean clusterRuntimeUpgradeNeeded =
                isUpdateNeeded(currentRuntimeBuildNumber, NullUtil.getIfNotNull(targetIm.getStackDetails(), ImageStackDetails::getStackBuildNumber));

        stackUpdater.updateStackStatus(stackId,
                rollingUpgradeEnabled ? CLUSTER_ROLLING_UPGRADE_FINISHED : DetailedStackStatus.CLUSTER_UPGRADE_FINISHED,
                "Cluster was successfully upgraded.");

        Optional<String> stackVersion = getStackVersionFromImage(targetIm);
        stackVersion.ifPresentOrElse(s -> stackUpdater.updateStackVersion(stackId, s),
                () -> LOGGER.warn("Cluster runtime could not be upgraded for stack with id {}", stackId));
        if (clusterRuntimeUpgradeNeeded) {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED, clusterStackVersion);
        } else {
            flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_UPGRADE_FINISHED_NOVERSION);
        }
    }

    public boolean isPatchUpgrade(Map<String, String> currentImagePackages, Map<String, String> targetImagePackages) {
        return currentImagePackages != null && targetImagePackages != null
                && StringUtils.isNoneBlank(currentImagePackages.get(STACK.getKey()), targetImagePackages.get(STACK.getKey()))
                && currentImagePackages.get(STACK.getKey()).equals(targetImagePackages.get(STACK.getKey()))
                && StringUtils.isNoneBlank(currentImagePackages.get(CDH_BUILD_NUMBER.getKey()), targetImagePackages.get(CDH_BUILD_NUMBER.getKey()))
                && !currentImagePackages.get(CDH_BUILD_NUMBER.getKey()).equals(targetImagePackages.get(CDH_BUILD_NUMBER.getKey()));
    }

    public boolean isRuntimeUpgradeNecessary(Set<ClouderaManagerProduct> upgradeCandidateProducts) {
        return CollectionUtils.isNotEmpty(upgradeCandidateProducts);
    }

    private Optional<String> getStackVersionFromImage(Image image) {
        return Optional.ofNullable(image.getStackDetails())
                .map(ImageStackDetails::getVersion);
    }

    public void handleUpgradeClusterFailure(long stackId, String errorReason, DetailedStackStatus detailedStatus, boolean rollingUpgradeEnabled) {
        switch (detailedStatus) {
        case CLUSTER_MANAGER_UPGRADE_FAILED:
            stackUpdater.updateStackStatus(stackId, rollingUpgradeEnabled ? CLUSTER_MANAGER_ROLLING_UPGRADE_FAILED : detailedStatus, errorReason);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), CLUSTER_MANAGER_UPGRADE_FAILED, errorReason);
            break;
        case CLUSTER_UPGRADE_FAILED:
        default:
            stackUpdater.updateStackStatus(stackId, rollingUpgradeEnabled ? CLUSTER_ROLLING_UPGRADE_FAILED : detailedStatus, errorReason);
            flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), CLUSTER_UPGRADE_FAILED, errorReason);
        }
    }

    private String getCurrentStackBuildNumber(Map<String, String> currentImagePackages) {
        return currentImagePackages.get(CDH_BUILD_NUMBER.getKey());
    }

    private boolean isUpdateNeeded(String currentBuildNumber, String targetBuildNumber) {
        if (StringUtils.isEmpty(currentBuildNumber) || StringUtils.isEmpty(targetBuildNumber)) {
            return true;
        }
        return !currentBuildNumber.equals(targetBuildNumber);
    }

}
