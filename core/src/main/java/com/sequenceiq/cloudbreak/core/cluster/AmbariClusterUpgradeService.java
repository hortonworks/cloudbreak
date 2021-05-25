package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.json.Json;
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
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariSecurityConfigProvider;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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
    private StackService stackService;

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private PaywallCredentialService paywallCredentialService;

    public void upgradeCluster(Long stackId) throws CloudbreakOrchestratorException {
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
                AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(cluster.getId());
                Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
                Map<String, Object> credentials = new HashMap<>();
                credentials.put("username", ambariSecurityConfigProvider.getAmbariUserName(cluster));
                credentials.put("password", ambariSecurityConfigProvider.getAmbariPassword(cluster));
                servicePillar.put("ambari-credentials", new SaltPillarProperties("/ambari/credentials.sls", singletonMap("ambari", credentials)));
                if (ambariRepo != null) {
                    Json blueprint = new Json(cluster.getBlueprint().getBlueprintText());
                    servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", Map.of("ambari", singletonMap(
                            "repo", Map.of(
                                    "baseUrl", ambariRepo.getBaseUrl(),
                                    "gpgKeyUrl", ambariRepo.getGpgKeyUrl(),
                                    "predefined", ambariRepo.getPredefined(),
                                    "version", ambariRepo.getVersion(),
                                    "stack_version", blueprint.getValue("Blueprints.stack_version"),
                                    "stack_type", blueprint.getValue("Blueprints.stack_name").toString().toLowerCase()
                            )),
                            "paywall", paywallCredentialService.getCredential())));
                }
                paywallCredentialService.setPaywallCredentialMoved(servicePillar);
                SaltConfig pillar = new SaltConfig(servicePillar);
                hostOrchestrator.upgradeAmbari(gatewayConfig, gatewayFQDN, stackUtil.collectNodes(stack), pillar, exitCriteriaModel);
            } else {
                throw new UnsupportedOperationException("Ambari upgrade works only with host orchestrator");
            }
        } catch (CloudbreakException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

}
