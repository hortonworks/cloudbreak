package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
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
    private HostGroupService hostGroupService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private ResourceService resourceService;

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
            clusterService.updateInstancesToRunning(stack.getCluster().getId(), hostsPerHostGroup);
        } else {
            LOGGER.info("Please implement {} orchestrator because it is not on classpath.", orchestrator.getType());
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
        ambariClusterConnector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId));
    }

    public void uploadRecipesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(resourceService.getNotInstanceRelatedByStackId(stackId));
        LOGGER.info("Start executing pre recipes");
        HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName);
        Set<HostGroup> hostGroups = hostGroupService.getByClusterWithRecipes(stack.getCluster().getId());
        recipeEngine.uploadUpscaleRecipes(stack, hostGroup, hostGroups);
    }

    public void installServicesOnNewHosts(Long stackId, String hostGroupName) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start installing Ambari services");
        ambariClusterConnector.upscaleCluster(stack, hostGroupName);
    }

    public void executePostRecipesOnNewHosts(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Start executing post recipes");
        recipeEngine.executePostInstallRecipes(stack);
    }
}
