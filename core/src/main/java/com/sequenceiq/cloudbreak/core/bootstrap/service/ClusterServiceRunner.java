package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceRunner.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private InstanceMetadataService instanceMetadataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterContainerRunner containerRunner;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void runAmbariServices(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.containerOrchestrator()) {
            Map<String, List<Container>> containers = containerRunner.runClusterContainers(stack);
            Container ambariServerContainer = containers.get(DockerContainer.AMBARI_SERVER.name()).stream().findFirst().get();
            String ambariServerIp = ambariServerContainer.getHost();
            HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack, ambariServerIp);
            clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
            Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
            for (Entry<String, List<Container>> containersEntry : containers.entrySet()) {
                List<String> hostNames = new ArrayList<>();
                for (Container container : containersEntry.getValue()) {
                    hostNames.add(container.getHost());
                }
                hostsPerHostGroup.put(containersEntry.getKey(), hostNames);
            }
            clusterService.updateHostMetadata(cluster.getId(), hostsPerHostGroup, HostMetadataState.CONTAINER_RUNNING);
        } else if (orchestratorType.hostOrchestrator()) {
            hostRunner.runAmbariServices(stack, cluster);
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
            HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack, gatewayIp);
            clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
            Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
            for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
                String groupName = instanceMetaData.getInstanceGroup().getGroupName();
                if (!hostsPerHostGroup.keySet().contains(groupName)) {
                    hostsPerHostGroup.put(groupName, new ArrayList<>());
                }
                hostsPerHostGroup.get(groupName).add(instanceMetaData.getDiscoveryFQDN());
            }
            clusterService.updateHostMetadata(cluster.getId(), hostsPerHostGroup, HostMetadataState.SERVICES_RUNNING);
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
    }

    public void updateSaltState(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.containerOrchestrator()) {
            LOGGER.info("Container orchestrator is not supported for this action.");
        } else {
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            hostRunner.runAmbariServices(stack, cluster);
        }
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        if (orchestratorTypeResolver.resolveType(orchestrator.getType()).hostOrchestrator()) {
            return hostRunner.changePrimaryGateway(stack);
        }
        throw new CloudbreakException(String.format("Change primary gateway is not supported on orchestrator %s", orchestrator.getType()));
    }

    private HttpClientConfig buildAmbariClientConfig(Stack stack, String gatewayPublicIp) throws CloudbreakSecuritySetupException {
        Map<InstanceGroupType, InstanceStatus> newStatusByGroupType = new HashMap<>();
        newStatusByGroupType.put(InstanceGroupType.GATEWAY, InstanceStatus.REGISTERED);
        newStatusByGroupType.put(InstanceGroupType.CORE, InstanceStatus.UNREGISTERED);
        instanceMetadataService.updateInstanceStatus(stack.getInstanceGroups(), newStatusByGroupType);
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp);
    }
}
