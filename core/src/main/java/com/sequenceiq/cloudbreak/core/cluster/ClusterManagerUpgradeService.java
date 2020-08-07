package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
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

    public void deactivateUnusedComponents(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        Set<ClusterComponent> components = clusterComponentConfigProvider.getComponentsByClusterId(cluster.getId()).stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS == clusterComponent.getComponentType())
                .collect(Collectors.toSet());
        Map<String, ClusterComponent> cmProductMap = new HashMap<>();
        Set<ClouderaManagerProduct> cmProducts = new HashSet<>();
        for (ClusterComponent clusterComponent : components) {
            ClouderaManagerProduct product = clusterComponent.getAttributes().getSilent(ClouderaManagerProduct.class);
            cmProductMap.put(product.getName(), clusterComponent);
            cmProducts.add(product);
        }
        cmProducts = parcelService.filterParcelsByBlueprint(cmProducts, cluster.getBlueprint());
        Set<ClusterComponent> blueprintProducts = cmProducts.stream().map(cmp -> cmProductMap.get(cmp.getName())).collect(Collectors.toSet());
        clusterApiConnectors.getConnector(stack).deactivateUnUsedComponents(blueprintProducts);
    }

    public void upgradeClusterManager(Long stackId) throws CloudbreakOrchestratorException, CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stopClusterServices(stack);
        upgradeClusterManager(stack);
    }

    private void upgradeClusterManager(Stack stack) throws CloudbreakOrchestratorException {
        Cluster cluster = stack.getCluster();
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.getGateway() != null);
        Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
        SaltConfig pillar = createSaltConfig(cluster);
        hostOrchestrator.upgradeClusterManager(gatewayConfig, gatewayFQDN, stackUtil.collectReachableNodes(stack), pillar, exitCriteriaModel);
    }

    private void stopClusterServices(Stack stack) throws CloudbreakException {
        clusterApiConnectors.getConnector(stack).stopCluster(true);
    }

    private SaltConfig createSaltConfig(Cluster cluster) {
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
        servicePillar.put("cloudera-manager-repo", new SaltPillarProperties("/cloudera-manager/repo.sls",
                singletonMap("cloudera-manager", singletonMap("repo", clouderaManagerRepo))));
        clusterHostServiceRunner.decoratePillarWithClouderaManagerSettings(servicePillar, clouderaManagerRepo);
        return new SaltConfig(servicePillar);
    }
}
