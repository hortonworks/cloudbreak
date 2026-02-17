package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.OsType;

@Service
public class ClusterManagerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private OsChangeService osChangeService;

    @Inject
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    void upgradeClouderaManager(StackDto stackDto, ClouderaManagerRepo clouderaManagerRepo, OsType originalOsTye) throws CloudbreakOrchestratorException {
        StackView stack = stackDto.getStack();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getClusterId());

        clouderaManagerRepo = updateCmRepoInCaseOfOsChange(clouderaManagerRepo, originalOsTye, stack.getId());

        SaltConfig pillar = createSaltConfig(stackDto, primaryGatewayConfig, clouderaManagerRepo);
        Set<String> allNode = stackUtil.collectNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            Set<Node> reachableNodes = stackUtil.collectReachableAndCheckNecessaryNodes(stackDto, allNode);
            LOGGER.debug("Starting to upgrade cluster manager.");
            hostOrchestrator.upgradeClusterManager(primaryGatewayConfig, gatewayFQDN, reachableNodes, pillar, exitCriteriaModel);
            LOGGER.debug("Cluster manager upgrade finished.");
        } catch (NodesUnreachableException e) {
            String errorMessage = "Can not upgrade cluster manager because the configuration management service is not responding on these nodes: "
                    + e.getUnreachableNodes();
            LOGGER.error(errorMessage);
            throw new CloudbreakRuntimeException(errorMessage, e);
        }
    }

    private ClouderaManagerRepo updateCmRepoInCaseOfOsChange(ClouderaManagerRepo clouderaManagerRepo, OsType originalOsTye, Long stackId) {
        Image targetImage = getTargetImage(stackId);
        OsType targetOsType = OsType.getByOsTypeString(targetImage.getOsType());
        String architecture = targetImage.getArchitecture();
        clouderaManagerRepo = osChangeService.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, originalOsTye, targetOsType, architecture);
        return clouderaManagerRepo;
    }

    private Image getTargetImage(Long stackId) {
        return clusterUpgradeTargetImageService.findTargetImage(stackId)
                .orElseThrow(() -> new CloudbreakRuntimeException("Target image not found"));
    }

    private SaltConfig createSaltConfig(StackDto stackDto, GatewayConfig primaryGatewayConfig, ClouderaManagerRepo clouderaManagerRepo) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Optional<String> license = clusterHostServiceRunner.decoratePillarWithClouderaManagerLicense(stackDto.getStack(), servicePillar);
        clusterHostServiceRunner.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, license);
        servicePillar.putAll(clusterHostServiceRunner.createPillarWithClouderaManagerSettings(clouderaManagerRepo, stackDto, primaryGatewayConfig));
        return new SaltConfig(servicePillar);
    }
}
