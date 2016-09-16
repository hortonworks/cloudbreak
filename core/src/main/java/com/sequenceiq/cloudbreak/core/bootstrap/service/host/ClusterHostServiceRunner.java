package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class ClusterHostServiceRunner {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private GatewayConfigService gatewayConfigService;
    @Inject
    private ConversionService conversionService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Inject
    private ComponentConfigProvider componentConfigProvider;
    @Inject
    private BlueprintUtils blueprintUtils;

    public void runAmbariServices(Stack stack) throws CloudbreakException {
        try {
            Set<Node> nodes = collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
            Cluster cluster = stack.getCluster();
            Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
            if (cluster.isSecure()) {
                Map<String, Object> krb = new HashMap<>();
                Map<String, String> kerberosConf = new HashMap<>();
                kerberosConf.put("masterKey", cluster.getKerberosMasterKey());
                kerberosConf.put("user", cluster.getKerberosAdmin());
                kerberosConf.put("password", cluster.getKerberosPassword());
                krb.put("kerberos", kerberosConf);
                servicePillar.put("kerberos", new SaltPillarProperties("/kerberos/init.sls", krb));
            }
            servicePillar.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.cloudPlatform())));
            AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(stack.getId());
            if (ambariRepo != null) {
                servicePillar.put("ambari-repo", new SaltPillarProperties("/ambari/repo.sls", singletonMap("ambari", singletonMap("repo", ambariRepo))));
            }
            AmbariDatabase ambariDb = componentConfigProvider.getAmbariDatabase(stack.getId());
            servicePillar.put("ambari-database", new SaltPillarProperties("/ambari/database.sls", singletonMap("ambari", singletonMap("database", ambariDb))));
            LdapConfig ldapConfig = cluster.getLdapConfig();
            if (ldapConfig != null && blueprintUtils.containsComponent(cluster.getBlueprint(), "KNOX_GATEWAY")) {
                servicePillar.put("ldap", new SaltPillarProperties("/ldap/init.sls", singletonMap("ldap", ldapConfig)));
            }
            SaltPillarConfig saltPillarConfig = new SaltPillarConfig(servicePillar);
            hostOrchestrator.runService(gatewayConfig, nodes, saltPillarConfig, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException | IOException e) {
            throw new CloudbreakException(e);
        }
    }

    public Map<String, String> addAmbariServices(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Map<String, String> candidates;
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            Cluster cluster = stack.getCluster();
            candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
            Set<Node> allNodes = collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack);
            hostOrchestrator.runService(gatewayConfig, allNodes, new SaltPillarConfig(), clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
        return candidates;
    }

    private Map<String, String> collectUpscaleCandidates(Long clusterId, String hostGroupName, Integer adjustment) {
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
        if (hostGroup.getConstraint().getInstanceGroup() != null) {
            Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
            Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetaDataRepository.findUnusedHostsInInstanceGroup(instanceGroupId);
            Map<String, String> hostNames = new HashMap<>();
            for (InstanceMetaData instanceMetaData : unusedHostsInInstanceGroup) {
                hostNames.put(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getPrivateIp());
                if (hostNames.size() >= adjustment) {
                    break;
                }
            }
            return hostNames;
        }
        return null;
    }

    private Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                agents.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryFQDN()));
            }
        }
        return agents;
    }

}
