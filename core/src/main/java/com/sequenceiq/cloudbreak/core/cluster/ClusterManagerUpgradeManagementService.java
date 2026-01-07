package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@Service
public class ClusterManagerUpgradeManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeManagementService.class);

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

    public void upgradeClusterManager(Long stackId, boolean rollingUpgradeEnabled, boolean runtimeUpgradeNecessary, String targetRuntimeVersion)
            throws CloudbreakOrchestratorException, CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDto.getCluster().getId());
        boolean clusterManagerUpgradeNecessary = isClusterManagerUpgradeNecessary(clouderaManagerRepo.getFullVersion(), stackDto);
        stopClusterServicesIfNecessary(rollingUpgradeEnabled, runtimeUpgradeNecessary, stackDto, targetRuntimeVersion);
        if (clusterManagerUpgradeNecessary) {
            clusterUpgradeService.upgradeClusterManager(stackId);
            clusterManagerUpgradeService.upgradeClouderaManager(stackDto, clouderaManagerRepo);
            validateCmVersionAfterUpgrade(stackDto, clouderaManagerRepo);
            startClusterManagerServices(stackDto);
            reconfigureClusterManager(stackDto);
        } else {
            LOGGER.debug("Skipping Cloudera Manager upgrade because the version {} is already installed.", clouderaManagerRepo.getFullVersion());
        }
    }

    private boolean isClusterManagerUpgradeNecessary(String targetVersion, StackDto stackDto) {
        Optional<String> installedCmVersion = getInstalledCmVersion(stackDto);
        LOGGER.debug("Comparing installed CM version: {} with the target: {}", installedCmVersion, targetVersion);
        return installedCmVersion.isEmpty() || !targetVersion.equals(installedCmVersion.get());
    }

    private Optional<String> getInstalledCmVersion(StackDto stackDto) {
        try {
            return cmServerQueryService.queryCmVersion(stackDto);
        } catch (Exception e) {
            LOGGER.error("Failed while fetching CM version", e);
            return Optional.empty();
        }
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
        return Strings.CS.removeEnd(version, "p");
    }

    private void stopClusterServicesIfNecessary(boolean rollingUpgradeEnabled, boolean runtimeUpgradeNecessary, StackDto stackDto, String targetRuntimeVersion)
            throws CloudbreakException {
        boolean skipStopForDlUpgradeTo732 = stackDto.getStack().isDatalake()
                && StringUtils.isNotBlank(targetRuntimeVersion)
                && CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited(targetRuntimeVersion, CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2);
        if (!runtimeUpgradeNecessary || rollingUpgradeEnabled || skipStopForDlUpgradeTo732) {
            LOGGER.debug("Not necessary to stop services because the rolling upgrade option is: {} or runtime upgrade is necessary: {} or skipping for DL {}",
                    rollingUpgradeEnabled, runtimeUpgradeNecessary, skipStopForDlUpgradeTo732);
        } else {
            LOGGER.debug("Stopping cluster services.");
            clusterApiConnectors.getConnector(stackDto).stopCluster(true);
        }
    }

    private void startClusterManagerServices(StackDto stackDto) throws CloudbreakException {
        LOGGER.debug("Starting cluster manager services after CM upgrade.");
        clusterApiConnectors.getConnector(stackDto).startClusterManagerAndAgents();
    }

    private void reconfigureClusterManager(StackDto stackDto) {
        LOGGER.debug("Reconfigure cluster manager after CM upgrade.");
        clusterApiConnectors.getConnector(stackDto).clusterSetupService().updateConfig();
    }
}
