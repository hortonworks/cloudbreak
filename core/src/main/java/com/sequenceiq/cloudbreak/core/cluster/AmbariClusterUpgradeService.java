package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariAuthenticationProvider;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class AmbariClusterUpgradeService {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    @Inject
    private AmbariAuthenticationProvider ambariAuthenticationProvider;

    @Inject
    private StackUtil stackUtil;

    public void upgradeCluster(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(stack.getOrchestrator().getType());
            if (orchestratorType.hostOrchestrator()) {
                HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
                InstanceGroup gatewayInstanceGroup = stack.getGatewayInstanceGroup();
                InstanceMetaData gatewayInstance = gatewayInstanceGroup.getInstanceMetaData().iterator().next();
                GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gatewayInstance, cluster.getEnableKnoxGateway());
                Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
                ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId());
                AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(cluster.getId());
                Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
                Map<String, Object> credentials = new HashMap<>();
                credentials.put("username", ambariAuthenticationProvider.getAmbariUserName(cluster));
                credentials.put("password", ambariAuthenticationProvider.getAmbariPassword(cluster));
                servicePillar.put("ambari-credentials", new SaltPillarProperties("/ambari/credentials.sls", singletonMap("ambari", credentials)));
                if (ambariRepo != null) {
                    servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", singletonMap("ambari", singletonMap("repo", ambariRepo))));
                }
                SaltPillarConfig pillar = new SaltPillarConfig(servicePillar);
                hostOrchestrator.upgradeAmbari(gatewayConfig, gatewayFQDN, stackUtil.collectNodes(stack), pillar, exitCriteriaModel);
            } else {
                throw new UnsupportedOperationException("Ambari upgrade works only with host orchestrator");
            }
        } catch (CloudbreakException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

}
