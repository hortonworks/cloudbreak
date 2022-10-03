package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator.CsdParcelDecorator;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterManagerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private CsdParcelDecorator csdParcelDecorator;

    @Inject
    private CmServerQueryService cmServerQueryService;

    public void upgradeClusterManager(Long stackId, boolean runtimeServicesStartNeeded) throws CloudbreakOrchestratorException, CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        stopClusterServices(stackDto);
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDto.getCluster().getId());
        upgradeClusterManager(stackDto, clouderaManagerRepo);
        if (runtimeServicesStartNeeded) {
            LOGGER.info("Starting cluster runtime services after CM upgrade, it's needed if cluster runtime version hasn't been changed");
            startClusterServices(stackDto);
        } else {
            LOGGER.info("Runtime services won't be started after CM upgrade, it's not needed if cluster runtime version has been changed");
        }
        validateCmVersionAfterUpgrade(stackDto, clouderaManagerRepo);
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

    private void upgradeClusterManager(StackDto stackDto, ClouderaManagerRepo clouderaManagerRepo) throws CloudbreakOrchestratorException {
        StackView stack = stackDto.getStack();
        ClusterView cluster = stackDto.getCluster();
        InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getGatewayConfig(stack, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
        SaltConfig pillar = createSaltConfig(stackDto, primaryGatewayConfig, clouderaManagerRepo);
        Set<String> allNode = stackUtil.collectNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            Set<Node> reachableNodes = stackUtil.collectAndCheckReachableNodes(stackDto, allNode);
            hostOrchestrator.upgradeClusterManager(primaryGatewayConfig, gatewayFQDN, reachableNodes, pillar, exitCriteriaModel);
        } catch (NodesUnreachableException e) {
            String errorMessage = "Can not upgrade cluster manager because the configuration management service is not responding on these nodes: "
                    + e.getUnreachableNodes();
            LOGGER.error(errorMessage);
            throw new CloudbreakRuntimeException(errorMessage, e);
        }
    }

    private void stopClusterServices(StackDto stackDto) throws CloudbreakException {
        clusterApiConnectors.getConnector(stackDto).stopCluster(true);
    }

    private void startClusterServices(StackDto stackDto) throws CloudbreakException {
        clusterApiConnectors.getConnector(stackDto).startCluster();
    }

    private SaltConfig createSaltConfig(StackDto stackDto, GatewayConfig primaryGatewayConfig, ClouderaManagerRepo clouderaManagerRepo) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Optional<String> license = clusterHostServiceRunner.decoratePillarWithClouderaManagerLicense(stackDto.getStack(), servicePillar);
        clusterHostServiceRunner.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, license);
        servicePillar.putAll(clusterHostServiceRunner.createPillarWithClouderaManagerSettings(clouderaManagerRepo, stackDto, primaryGatewayConfig));
        csdParcelDecorator.decoratePillarWithCsdParcels(stackDto, servicePillar);
        return new SaltConfig(servicePillar);
    }
}
