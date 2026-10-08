package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
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
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterManagerUpgradePreparationStateParamsProvider clusterManagerUpgradePreparationStateParamsProvider;

    public void downloadCmPackages(Long stackId, ClusterUpgradeProperties properties) throws Exception {
        StackDto stack = stackDtoService.getById(stackId);
        Long clusterId = stack.getCluster().getId();
        ClouderaManagerRepo currentClouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(clusterId);
        String candidateCmBuildNumber = properties.targetImage().packageVersions().get(ImagePackageVersion.CM_BUILD_NUMBER.getKey());
        if (StringUtils.equals(currentClouderaManagerRepo.getBuildNumber(), candidateCmBuildNumber)) {
            LOGGER.debug("Cloudera Manager version is the same as the current one, no need to download CM packages");
        } else {
            eventService.fireCloudbreakEvent(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_DOWNLOAD_CM_PACKAGES);
            LOGGER.debug("Downloading CM packages for image {} from repo {}", properties.targetImage().imageId(),
                    properties.targetImage().clouderaManagerRepo());
            SaltConfig saltConfig = createSaltConfig(properties);
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

    private SaltConfig createSaltConfig(ClusterUpgradeProperties properties) {
        return new SaltConfig(clusterManagerUpgradePreparationStateParamsProvider.createParamsForCmPackageDownload(properties));
    }
}
