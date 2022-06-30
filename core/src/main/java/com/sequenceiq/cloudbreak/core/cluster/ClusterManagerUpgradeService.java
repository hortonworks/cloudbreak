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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
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
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.cloudbreak.util.NodesUnreachableException;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterManagerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradeService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackService stackService;

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
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stopClusterServices(stack);
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        upgradeClusterManager(stack, clouderaManagerRepo);
        if (runtimeServicesStartNeeded) {
            LOGGER.info("Starting cluster runtime services after CM upgrade, it's needed if cluster runtime version hasn't been changed");
            startClusterServices(stack);
        } else {
            LOGGER.info("Runtime services won't be started after CM upgrade, it's not needed if cluster runtime version has been changed");
        }
        validateCmVersionAfterUpgrade(stack, clouderaManagerRepo);
    }

    private void validateCmVersionAfterUpgrade(Stack stack, ClouderaManagerRepo clouderaManagerRepo) {
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

    private void upgradeClusterManager(Stack stack, ClouderaManagerRepo clouderaManagerRepo) throws CloudbreakOrchestratorException {
        Cluster cluster = stack.getCluster();
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.getGateway() != null);
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
        SaltConfig pillar = createSaltConfig(stack, primaryGatewayConfig, clouderaManagerRepo);
        Set<String> allNode = stackUtil.collectNodes(stack).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            Set<Node> reachableNodes = stackUtil.collectAndCheckReachableNodes(stack, allNode);
            hostOrchestrator.upgradeClusterManager(primaryGatewayConfig, gatewayFQDN, reachableNodes, pillar, exitCriteriaModel);
        } catch (NodesUnreachableException e) {
            String errorMessage = "Can not upgrade cluster manager because the configuration management service is not responding on these nodes: "
                    + e.getUnreachableNodes();
            LOGGER.error(errorMessage);
            throw new CloudbreakRuntimeException(errorMessage, e);
        }
    }

    private void stopClusterServices(Stack stack) throws CloudbreakException {
        clusterApiConnectors.getConnector(stack).stopCluster(true);
    }

    private void startClusterServices(Stack stack) throws CloudbreakException {
        clusterApiConnectors.getConnector(stack).startCluster();
    }

    private SaltConfig createSaltConfig(Stack stack, GatewayConfig primaryGatewayConfig, ClouderaManagerRepo clouderaManagerRepo) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        Optional<String> license = clusterHostServiceRunner.decoratePillarWithClouderaManagerLicense(stack.getId(), servicePillar);
        clusterHostServiceRunner.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, license);
        servicePillar.putAll(clusterHostServiceRunner.createPillarWithClouderaManagerSettings(clouderaManagerRepo, stack, primaryGatewayConfig));
        csdParcelDecorator.decoratePillarWithCsdParcels(stack, servicePillar);
        return new SaltConfig(servicePillar);
    }
}
