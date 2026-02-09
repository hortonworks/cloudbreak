package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;

@Component
public class ClouderaManagerCsdDownloaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCsdDownloaderService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 200;

    private static final String STATE = "cloudera/csd/init";

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private CsdParcelDecorator csdParcelDecorator;

    @Inject
    private ClusterManagerRestartService clusterManagerRestartService;

    public void downloadCsdFiles(StackDto stack, boolean clusterManagerUpgradeNecessary, Set<ClouderaManagerProduct> upgradeCandidateProducts,
            boolean upgradePreparation) {
        Map<String, SaltPillarProperties> pillarProperties = csdParcelDecorator.addCsdParcelsToServicePillar(upgradeCandidateProducts, upgradePreparation);
        if (pillarProperties.isEmpty()) {
            LOGGER.debug("There are no CSD files to download.");
        } else {
            try {
                clusterHostServiceRunner.redeployStates(stack);
                OrchestratorStateParams stateParams = saltStateParamsService.createStateParamsForReachableNodes(stack, STATE, MAX_RETRY, MAX_RETRY_ON_ERROR);
                hostOrchestrator.saveCustomPillars(new SaltConfig(pillarProperties), createExitCriteriaModel(stack), stateParams);
                LOGGER.debug("Running CSD file download with params {}", pillarProperties);
                hostOrchestrator.runOrchestratorState(stateParams);
                LOGGER.debug("CSD file package download finished");
            } catch (Exception e) {
                LOGGER.error("Failed to download CSD files.", e);
            }
            if (clusterManagerUpgradeNecessary) {
                LOGGER.debug("Cloudera Manager restart not necessary");
            } else {
                LOGGER.debug("Restarting Cloudera Manager to apply new CSD files.");
                clusterManagerRestartService.restartClouderaManager(stack);
            }
        }
    }

    private ClusterDeletionBasedExitCriteriaModel createExitCriteriaModel(StackDto stack) {
        return new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId());
    }
}
