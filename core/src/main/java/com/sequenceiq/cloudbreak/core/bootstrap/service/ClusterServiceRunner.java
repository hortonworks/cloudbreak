package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceRunner.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterContainerRunner containerRunner;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void runAmbariServices(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        Orchestrator orchestrator = stack.getOrchestrator();
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
            hostRunner.runClusterServices(stack, cluster);
            updateAmbariClientConfig(stack, cluster);
            Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
            for (InstanceMetaData instanceMetaData : stack.getNotDeletedInstanceMetaDataSet()) {
                String groupName = instanceMetaData.getInstanceGroup().getGroupName();
                if (!hostsPerHostGroup.keySet().contains(groupName)) {
                    hostsPerHostGroup.put(groupName, new ArrayList<>());
                }
                hostsPerHostGroup.get(groupName).add(instanceMetaData.getDiscoveryFQDN());
            }
            clusterService.updateHostMetadata(cluster.getId(), hostsPerHostGroup, HostMetadataState.SERVICES_RUNNING);
        } else {
            LOGGER.info("Please implement {} orchestrator because it is not on classpath.", orchestrator.getType());
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
    }

    public void updateAmbariClientConfig(Stack stack, Cluster cluster) {
        String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack, gatewayIp);
        clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
    }

    public void updateSaltState(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.containerOrchestrator()) {
            LOGGER.debug("Container orchestrator is not supported for this action.");
        } else {
            Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId());
            hostRunner.runClusterServices(stack, cluster);
        }
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        if (orchestratorTypeResolver.resolveType(orchestrator.getType()).hostOrchestrator()) {
            return hostRunner.changePrimaryGateway(stack);
        }
        throw new CloudbreakException(String.format("Change primary gateway is not supported on orchestrator %s", orchestrator.getType()));
    }

    private HttpClientConfig buildAmbariClientConfig(Stack stack, String gatewayPublicIp) {
        Map<InstanceGroupType, InstanceStatus> newStatusByGroupType = new EnumMap<>(InstanceGroupType.class);
        newStatusByGroupType.put(InstanceGroupType.GATEWAY, InstanceStatus.REGISTERED);
        newStatusByGroupType.put(InstanceGroupType.CORE, InstanceStatus.UNREGISTERED);
        instanceMetaDataService.updateInstanceStatus(stack.getInstanceGroups(), newStatusByGroupType);
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp);
    }
}
