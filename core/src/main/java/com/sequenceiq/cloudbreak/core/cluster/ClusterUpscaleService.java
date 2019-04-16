package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private HostMetadataService hostMetadataService;

    public void upscaleClusterManager(Long stackId, String hostGroupName, Integer scalingAdjustment, boolean primaryGatewayChanged) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start adding cluster containers");
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
        if (orchestratorType.containerOrchestrator()) {
            Map<String, List<Container>> containers = containerRunner.addClusterContainers(stackId, hostGroupName, scalingAdjustment);
            for (Entry<String, List<Container>> containersEntry : containers.entrySet()) {
                List<String> hostNames = containersEntry.getValue().stream().map(Container::getHost).collect(Collectors.toList());
                hostsPerHostGroup.put(containersEntry.getKey(), hostNames);
            }
            clusterService.updateHostMetadata(stack.getCluster().getId(), hostsPerHostGroup, HostMetadataState.CONTAINER_RUNNING);
        } else if (orchestratorType.hostOrchestrator()) {
            Map<String, String> hosts = hostRunner.addClusterServices(stackId, hostGroupName, scalingAdjustment);
            if (primaryGatewayChanged) {
                clusterServiceRunner.updateAmbariClientConfig(stack, stack.getCluster());
            }
            for (String hostName : hosts.keySet()) {
                if (!hostsPerHostGroup.keySet().contains(hostGroupName)) {
                    hostsPerHostGroup.put(hostGroupName, new ArrayList<>());
                }
                hostsPerHostGroup.get(hostGroupName).add(hostName);
            }
            clusterService.updateHostMetadata(stack.getCluster().getId(), hostsPerHostGroup, HostMetadataState.SERVICES_RUNNING);
        } else {
            LOGGER.info("Please implement {} orchestrator because it is not on classpath.", orchestrator.getType());
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
        Set<String> allHosts = new HashSet<>();
        for (Entry<String, List<String>> hostsPerHostGroupEntry : hostsPerHostGroup.entrySet()) {
            allHosts.addAll(hostsPerHostGroupEntry.getValue());
        }
        instanceMetaDataService.updateInstanceStatus(stack.getInstanceGroups(), InstanceStatus.UNREGISTERED, allHosts);
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Set<HostMetadata> hostsInCluster = hostMetadataService.findHostsInCluster(stack.getCluster().getId());
        connector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId), hostsInCluster);
    }

    public void uploadRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start executing pre recipes");
        HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
        recipeEngine.uploadUpscaleRecipes(stack, hostGroup, hostGroups);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
        LOGGER.debug("Start installing Ambari services");
        HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
        List<InstanceMetaData> metas = instanceMetaDataService.findAliveInstancesInInstanceGroup(instanceGroupId);
        recipeEngine.executePostAmbariStartRecipes(stack, Sets.newHashSet(hostGroup));
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        connector.upscaleCluster(hostGroup, hostMetadata, metas);
    }

    public void executePostRecipesOnNewHosts(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Start executing post recipes");
        recipeEngine.executePostInstallRecipes(stack, hostGroupService.getByCluster(stack.getCluster().getId()));
    }

    public Map<String, String> gatherInstalledComponents(Long stackId, String hostname) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start gathering installed components from ambari on host {}", hostname);
        return getClusterConnector(stack).gatherInstalledComponents(hostname);
    }

    public void ensureComponentsAreStopped(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Ensuring components are in stopped state in ambari on host {}", hostname);
        getClusterConnector(stack).ensureComponentsAreStopped(components, hostname);
    }

    public void initComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start init components in ambari on host {}", hostname);
        getClusterConnector(stack).initComponents(components, hostname);
    }

    public void stopComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start stop components in ambari on host {}", hostname);
        getClusterConnector(stack).stopComponents(components, hostname);
    }

    public void installComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start installing components in ambari on host {}", hostname);
        getClusterConnector(stack).installComponents(components, hostname);
    }

    public void regenerateKerberosKeytabs(Long stackId, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start regenerate kerberos keytabs in ambari on host {}", hostname);
        getClusterConnector(stack).regenerateKerberosKeytabs(hostname);
    }

    public void startComponents(Long stackId, Map<String, String> components, String hostname) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start components in ambari on host {}", hostname);
        getClusterConnector(stack).startComponents(components, hostname);
    }

    public void restartAll(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Restart all in ambari");
        getClusterConnector(stack).restartAll();
    }

    private ClusterApi getClusterConnector(Stack stack) {
        return clusterApiConnectors.getConnector(stack);
    }
}
