package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ClusterContainerRunner containerRunner;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private InstanceMetadataService instanceMetadataService;

    @Inject
    private HostGroupService hostGroupService;

    public void addClusterContainers(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        LOGGER.info("Start adding cluster containers");
        Stack stack = stackService.getById(stackId);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS, "Adding new containers to the cluster.");
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
        if (orchestratorType.containerOrchestrator()) {
            Map<String, List<Container>> containers = containerRunner.addClusterContainers(stackId, stack.cloudPlatform(), hostGroupName, scalingAdjustment);
            for (Map.Entry<String, List<Container>> containersEntry : containers.entrySet()) {
                List<String> hostNames = containersEntry.getValue().stream().map(Container::getHost).collect(Collectors.toList());
                hostsPerHostGroup.put(containersEntry.getKey(), hostNames);
            }
        } else if (orchestratorType.hostOrchestrator()) {
            Map<String, String> hosts = hostRunner.addAmbariServices(stackId, hostGroupName, scalingAdjustment);
            for (String hostName : hosts.keySet()) {
                if (!hostsPerHostGroup.keySet().contains(hostGroupName)) {
                    hostsPerHostGroup.put(hostGroupName, new ArrayList<>());
                }
                hostsPerHostGroup.get(hostGroupName).add(hostName);
            }
        } else {
            LOGGER.info(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
        clusterService.updateHostMetadata(stack.getCluster().getId(), hostsPerHostGroup);
        Set<String> allHosts = new HashSet<>();
        for (Map.Entry<String, List<String>> hostsPerHostGroupEntry : hostsPerHostGroup.entrySet()) {
            allHosts.addAll(hostsPerHostGroupEntry.getValue());
        }
        clusterService.updateHostCountWithAdjustment(stack.getCluster().getId(), hostGroupName, allHosts.size());
        if (!"BYOS".equals(stack.cloudPlatform())) {
            instanceMetadataService.updateInstanceStatus(stack.getInstanceGroups(), InstanceStatus.UNREGISTERED, allHosts);
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE, "New containers added to the cluster.");
    }

    public void installFsRecipes(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start upscale cluster");
        Stack stack = stackService.getById(stackId);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        ambariClusterConnector.installFsRecipes(stack, hostGroup);
    }

    public void waitForAmbariHosts(Long stackId) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start waiting for Ambari hosts");
        ambariClusterConnector.waitForAmbariHosts(stackService.getById(stackId));
    }

    public void configureSssd(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start configuring SSSD");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.configureSssd(stack, hostMetadata);
    }

    public void installRecipes(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start installing recipes");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.installRecipes(stack, hostGroup, hostMetadata);
    }

    public void executePreRecipes(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start executing pre recipes");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.executePreRecipes(stack, hostGroup, hostMetadata);
    }

    public void installServices(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start installing Ambari services");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.installServices(stack, hostGroup, hostMetadata);
    }

    public void executePostRecipes(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start executing post recipes");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.executePostRecipes(stack, hostGroup, hostMetadata);
    }

    public int updateMetadata(Long stackId, String hostGroupName) throws CloudbreakSecuritySetupException {
        LOGGER.info("Start update metadata");
        Stack stack = stackService.getById(stackId);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.updateFailedHostMetaData(hostMetadata);
        int failedHosts = 0;
        for (HostMetadata hostMeta : hostMetadata) {
            if (!"BYOS".equals(stack.cloudPlatform()) && hostGroup.getConstraint().getInstanceGroup() != null) {
                stackService.updateMetaDataStatus(stack.getId(), hostMeta.getHostName(), InstanceStatus.REGISTERED);
            }
            hostGroupService.updateHostMetaDataStatus(hostMeta.getId(), HostMetadataState.HEALTHY);
            if (hostMeta.getHostMetadataState() == HostMetadataState.UNHEALTHY) {
                failedHosts++;
            }
        }
        return failedHosts;
    }
}