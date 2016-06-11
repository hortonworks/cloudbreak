package com.sequenceiq.cloudbreak.core.cluster;

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
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class AmbariClusterUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterUpscaleService.class);

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

    @Inject
    private RecipeEngine recipeEngine;

    public void upscaleAmbari(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start adding cluster containers");
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
        if (orchestratorType.containerOrchestrator()) {
            Map<String, List<Container>> containers = containerRunner.addClusterContainers(stackId, hostGroupName, scalingAdjustment);
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
        ambariClusterConnector.waitForAmbariHosts(stackService.getById(stackId));
    }

    public void executePreRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start executing pre recipes");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        recipeEngine.executeUpscalePreInstall(stack, hostGroup, hostMetadata);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start installing Ambari services");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.installServices(stack, hostGroup, hostMetadata);
    }

    public void executePostRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Start executing post recipes");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        recipeEngine.executeUpscalePostInstall(stack, hostMetadata);
    }
}
