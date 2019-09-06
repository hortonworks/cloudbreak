package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
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
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private RecipeEngine recipeEngine;

    public void upscaleAmbari(Long stackId, String hostGroupName, Integer scalingAdjustment) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start adding cluster containers");
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();
        if (orchestratorType.hostOrchestrator()) {
            Map<String, String> hosts = hostRunner.addAmbariServices(stackId, hostGroupName, scalingAdjustment);
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
        ambariClusterConnector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId));
    }

    public void uploadRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start executing pre recipes");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
        recipeEngine.uploadUpscaleRecipes(stack, hostGroup, hostGroups);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start installing Ambari services");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
        Set<HostMetadata> hostMetadata = hostGroupService.findEmptyHostMetadataInHostGroup(hostGroup.getId());
        ambariClusterConnector.upscaleCluster(stack, hostGroup, hostMetadata);
    }

    public void executePostRecipesOnNewHosts(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start executing post recipes");
        recipeEngine.executePostInstallRecipes(stack, hostGroupService.getByCluster(stack.getCluster().getId()));
    }
}
