package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.HostServiceConfigService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostService;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.HostServiceType;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.model.ServiceInfo;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.HostServiceService;

@Component
public class ClusterHostServiceRunner {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private HostServiceConfigService hostServiceConfigService;
    @Inject
    private HostServiceService hostServiceService;
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
            Set<String> agents = initializeAmbariAgentServices(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                    gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp());
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
            SaltPillarConfig saltPillarConfig = new SaltPillarConfig(servicePillar);
            hostOrchestrator.runService(gatewayConfig, agents, saltPillarConfig, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
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
            Set<String> agents = initializeNewAmbariAgentServices(stack, candidates);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                    gatewayInstance.getPublicIpWrapper(), stack.getGatewayPort(), gatewayInstance.getPrivateIp());
            hostOrchestrator.runService(gatewayConfig, agents, new SaltPillarConfig(), clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
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

    private Set<String> initializeNewAmbariAgentServices(Stack stack, Map<String, String> candidates) throws CloudbreakException,
            CloudbreakOrchestratorException {
        Set<String> agents = new HashSet<>();
        Map<String, List<ServiceInfo>> serviceInfos = new HashMap<>();
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        for (Map.Entry<String, String> entry : candidates.entrySet()) {
            agents.add(entry.getValue());
            ServiceInfo agentServiceInfo = new ServiceInfo(HostServiceType.AMBARI_AGENT.getName(), entry.getKey());
            serviceInfos.put(entry.getKey(), Arrays.asList(agentServiceInfo));
        }
        saveHostServices(serviceInfos, cluster);
        return agents;
    }

    private Set<String> initializeAmbariAgentServices(Stack stack) throws CloudbreakException, CloudbreakOrchestratorException {
        Set<String> agents = new HashSet<>();
        Map<String, List<ServiceInfo>> serviceInfos = new HashMap<>();
        InstanceGroup gatewayInstanceGroup = stack.getGatewayInstanceGroup();
        InstanceMetaData next = gatewayInstanceGroup.getInstanceMetaData().iterator().next();
        ServiceInfo serverServiceInfo = new ServiceInfo(HostServiceType.AMBARI_SERVER.getName(), next.getDiscoveryFQDN());
        serviceInfos.put(next.getPrivateIp(), Arrays.asList(serverServiceInfo));
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getInstanceGroupType().equals(InstanceGroupType.CORE)) {
                for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                    agents.add(instanceMetaData.getPrivateIp());
                    ServiceInfo agentServiceInfo = new ServiceInfo(HostServiceType.AMBARI_AGENT.getName(), instanceMetaData.getDiscoveryFQDN());
                    serviceInfos.put(instanceMetaData.getPrivateIp(), Arrays.asList(agentServiceInfo));
                }
            }
        }
        saveHostServices(serviceInfos, cluster);
        return agents;
    }

    private Map<String, List<HostService>> saveHostServices(Map<String, List<ServiceInfo>> serviceInfo, Cluster cluster) {
        Map<String, List<HostService>> hostServices = new HashMap<>();
        for (Map.Entry<String, List<ServiceInfo>> serviceInfoEntry : serviceInfo.entrySet()) {
            List<HostService> hostGroupHostServices = convert(serviceInfoEntry.getValue(), cluster);
            hostServices.put(serviceInfoEntry.getKey(), hostGroupHostServices);
            hostServiceService.save(hostGroupHostServices);
        }
        return hostServices;
    }

    private List<HostService> convert(List<ServiceInfo> serviceInfos, Cluster cluster) {
        List<HostService> services = new ArrayList<>();
        for (ServiceInfo source : serviceInfos) {
            HostService hostService = conversionService.convert(source, HostService.class);
            hostService.setCluster(cluster);
            services.add(hostService);
        }
        return services;
    }

}
