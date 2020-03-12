package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterManagerUpgradeService {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private StackUtil stackUtil;

    public void upgradeClusterManager(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
                GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.getGateway() != null);
                Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
                ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
                Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
                ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
                servicePillar.put("cloudera-manager-repo", new SaltPillarProperties("/cloudera-manager/repo.sls",
                        singletonMap("cloudera-manager", singletonMap("repo", clouderaManagerRepo))));

                SaltConfig pillar = new SaltConfig(servicePillar);
                hostOrchestrator.upgradeClusterManager(gatewayConfig, gatewayFQDN, stackUtil.collectNodes(stack), pillar, exitCriteriaModel);
            } else {
                throw new UnsupportedOperationException("Cloudera Manager upgrade supports host orchestrator only!");
            }
        } catch (CloudbreakException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

}
