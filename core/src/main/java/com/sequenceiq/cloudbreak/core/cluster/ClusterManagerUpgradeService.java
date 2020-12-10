package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
    private ParcelService parcelService;

    public void removeUnusedComponents(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<ClusterComponent> blueprintProducts = parcelService.getParcelComponentsByBlueprint(stack);
        clusterApiConnectors.getConnector(stack).removeUnusedParcels(blueprintProducts);
    }

    public void upgradeClusterManager(Long stackId, boolean runtimeServicesStartNeeded) throws CloudbreakOrchestratorException, CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stopClusterServices(stack);
        upgradeClusterManager(stack);
        if (runtimeServicesStartNeeded) {
            LOGGER.info("Starting cluster runtime services after CM upgrade, it's needed if cluster runtime version hasn't been changed");
            startClusterServices(stack);
        } else {
            LOGGER.info("Runtime services won't be started after CM upgrade, it's not needed if cluster runtime version has been changed");
        }
    }

    private void upgradeClusterManager(Stack stack) throws CloudbreakOrchestratorException {
        Cluster cluster = stack.getCluster();
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.getGateway() != null);
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
        SaltConfig pillar = createSaltConfig(stack.getId(), stack.getType(), cluster);
        hostOrchestrator.upgradeClusterManager(gatewayConfig, gatewayFQDN, stackUtil.collectReachableNodes(stack), pillar, exitCriteriaModel);
    }

    private void stopClusterServices(Stack stack) throws CloudbreakException {
        clusterApiConnectors.getConnector(stack).stopCluster(true);
    }

    private void startClusterServices(Stack stack) throws CloudbreakException {
        clusterApiConnectors.getConnector(stack).startCluster();
    }

    private SaltConfig createSaltConfig(Long stackId, StackType stackType, Cluster cluster) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
        Optional<String> license = clusterHostServiceRunner.decoratePillarWithClouderaManagerLicense(stackId, servicePillar);
        clusterHostServiceRunner.decoratePillarWithClouderaManagerRepo(clouderaManagerRepo, servicePillar, license);
        clusterHostServiceRunner.decoratePillarWithClouderaManagerSettings(servicePillar, clouderaManagerRepo);
        decorateWorkloadClusterPillarWithCsdDownloader(stackType, cluster, servicePillar);
        return new SaltConfig(servicePillar);
    }

    private void decorateWorkloadClusterPillarWithCsdDownloader(StackType stackType, Cluster cluster, Map<String, SaltPillarProperties> servicePillar) {
        if (StackType.WORKLOAD.equals(stackType)) {
            clusterHostServiceRunner.decoratePillarWithClouderaManagerCsds(cluster, servicePillar);
        } else {
            LOGGER.debug("Skipping the CSD downloading because the stack type is {}", stackType);
        }
    }
}
