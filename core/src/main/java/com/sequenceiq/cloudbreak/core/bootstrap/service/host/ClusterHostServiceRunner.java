package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
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
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class ClusterHostServiceRunner {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private ConversionService conversionService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private HostGroupRepository hostGroupRepository;
    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void runAmbariServices(Stack stack) throws CloudbreakException {
        try {
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            Set<Node> nodes = collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                    gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN());
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
            SaltPillarConfig saltPillarConfig = new SaltPillarConfig(servicePillar);
            hostOrchestrator.runService(gatewayConfig, nodes, saltPillarConfig, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public Map<String, String> addAmbariServices(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Map<String, String> candidates;
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            Cluster cluster = stack.getCluster();
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            candidates = collectUpscaleCandidates(cluster.getId(), hostGroupName, scalingAdjustment);
            Set<Node> allNodes = collectNodes(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                    gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp(), gatewayInstance.getDiscoveryFQDN());
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

    private Set<Node> collectNodes(Stack stack) throws CloudbreakException, CloudbreakOrchestratorException {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                agents.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryFQDN()));
            }
        }
        return agents;
    }

}
