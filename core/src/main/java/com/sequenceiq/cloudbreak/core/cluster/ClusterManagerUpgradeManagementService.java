package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class ClusterManagerUpgradeManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeManagementService.class);

    private static final String RUNTIME_VERSION_7_2_2 = "7.2.2";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private ClusterUpgradeService clusterUpgradeService;

    @Inject
    private ClusterManagerUpgradeService clusterManagerUpgradeService;

    public void upgradeClusterManager(Long stackId) throws CloudbreakOrchestratorException, CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDto.getCluster().getId());
        if (isClusterManagerUpgradeNecessary(clouderaManagerRepo.getFullVersion(), stackDto)) {
            stopClusterServicesIfNecessary(stackDto);
            clusterUpgradeService.upgradeClusterManager(stackDto.getId());
            clusterManagerUpgradeService.upgradeClouderaManager(stackDto, clouderaManagerRepo);
            validateCmVersionAfterUpgrade(stackDto, clouderaManagerRepo);
        } else {
            LOGGER.debug("Skipping Cloudera Manager upgrade because the version {} is already installed.", clouderaManagerRepo.getFullVersion());
        }
    }

    private boolean isClusterManagerUpgradeNecessary(String targetVersion, StackDto stackDto) {
        Optional<String> installedCmVersion = cmServerQueryService.queryCmVersion(stackDto);
        LOGGER.debug("Comparing installed CM version: {} with the target: {}", installedCmVersion, targetVersion);
        return installedCmVersion.isEmpty() || !targetVersion.equals(installedCmVersion.get());
    }

    private void validateCmVersionAfterUpgrade(StackDto stack, ClouderaManagerRepo clouderaManagerRepo) {
        Optional<String> cmVersion = cmServerQueryService.queryCmVersion(stack);
        if (cmVersion.isPresent()) {
            String cmVersionInRepoStripped = stripEndingMagicP(clouderaManagerRepo.getFullVersion());
            String cmVersionOnHostStripped = stripEndingMagicP(cmVersion.get());
            if (!cmVersionInRepoStripped.equals(cmVersionOnHostStripped)) {
                throw new CloudbreakServiceException(String.format("Cloudera manager version on host is [%s], while the expected is [%s]",
                        cmVersionOnHostStripped, cmVersionInRepoStripped));
            }
        } else {
            LOGGER.warn("Couldn't get CM version after upgrade");
        }
    }

    private String stripEndingMagicP(String version) {
        return StringUtils.removeEnd(version, "p");
    }

    private void stopClusterServicesIfNecessary(StackDto stackDto) throws CloudbreakException {
        if (isServiceStopNecessaryBasedOnRuntimeVersion(stackDto)) {
            LOGGER.debug("Stopping cluster services because the runtime version is lower or equals than {}"
                    + " and the upgrade is not supported with running services on this version.", RUNTIME_VERSION_7_2_2);
            clusterApiConnectors.getConnector(stackDto).stopCluster(true);
        } else {
            LOGGER.debug("Not necessary to stop services because the runtime version is higher than {}", RUNTIME_VERSION_7_2_2);
        }
    }

    private boolean isServiceStopNecessaryBasedOnRuntimeVersion(StackDto stackDto) {
        return new VersionComparator().compare(stackDto::getStackVersion, () -> RUNTIME_VERSION_7_2_2) < 1;
    }
}
