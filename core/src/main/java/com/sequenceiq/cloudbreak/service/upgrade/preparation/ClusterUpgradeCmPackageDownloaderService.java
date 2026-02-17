package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClusterUpgradeCmPackageDownloaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeCmPackageDownloaderService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 200;

    private static final String STATE = "cloudera/repo/upgrade-preparation";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterManagerUpgradePreparationStateParamsProvider clusterManagerUpgradePreparationStateParamsProvider;

    @Inject
    private ImageService imageService;

    public void downloadCmPackages(Long stackId, String targetImageId) throws Exception {
        StackDto stack = stackDtoService.getById(stackId);
        Image candidateImage = getImageFromCatalog(stackId, stack.getWorkspaceId(), targetImageId);
        Long clusterId = stack.getCluster().getId();
        ClouderaManagerRepo currentClouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(clusterId);
        if (currentClouderaManagerRepo.getBuildNumber().equals(candidateImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER))) {
            LOGGER.debug("Cloudera Manager version is the same as the current one, no need to download CM packages");
        } else {
            eventService.fireCloudbreakEvent(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_CM_PACKAGES);
            LOGGER.debug("Downloading CM packages based on image {}", targetImageId);
            SaltConfig saltConfig = createSaltConfig(candidateImage, stackId);
            clusterHostServiceRunner.redeployStates(stack);
            OrchestratorStateParams stateParams = createStateParams(stack);
            hostOrchestrator.saveCustomPillars(saltConfig, new ClusterDeletionBasedExitCriteriaModel(stackId, clusterId), stateParams);
            LOGGER.debug("Running CM package download with params {}", stateParams);
            hostOrchestrator.runOrchestratorState(stateParams);
            LOGGER.debug("CM package download finished");
        }
    }

    private OrchestratorStateParams createStateParams(StackDto stack) {
        return saltStateParamsService.createStateParamsForReachableNodes(stack, STATE, MAX_RETRY, MAX_RETRY_ON_ERROR);
    }

    private SaltConfig createSaltConfig(Image candidateImage, Long stackId) {
        return new SaltConfig(clusterManagerUpgradePreparationStateParamsProvider.createParamsForCmPackageDownload(candidateImage, stackId));
    }

    private Image getImageFromCatalog(Long stackId, Long workspaceId, String targetImageId)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image currentModelImage = imageService.getImage(stackId);
        return imageCatalogService.getImage(workspaceId, currentModelImage.getImageCatalogUrl(), currentModelImage.getImageCatalogName(),
                targetImageId).getImage();
    }
}
