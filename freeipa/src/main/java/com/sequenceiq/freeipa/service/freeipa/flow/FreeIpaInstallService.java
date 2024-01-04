package com.sequenceiq.freeipa.service.freeipa.flow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstallService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    public void installFreeIpa(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);

        installFreeIpa(stackId, stack, gatewayConfigs, allNodes);
    }

    private void installFreeIpa(Long stackId, Stack stack, List<GatewayConfig> gatewayConfigs, Set<Node> allNodes) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<RecipeModel> recipes = freeIpaRecipeService.getRecipes(stackId);
        if (!recipes.isEmpty()) {
            LOGGER.info("Recipes for stack: {}", recipes);
            Map<String, List<RecipeModel>> recipeMap = stack.getInstanceGroups().stream().map(InstanceGroup::getGroupName)
                    .collect(Collectors.toMap(instanceGroup -> instanceGroup, instanceGroup -> recipes));
            hostOrchestrator.uploadRecipes(gatewayConfigs, recipeMap, new StackBasedExitCriteriaModel(stackId));
            if (freeIpaRecipeService.hasRecipeType(recipes, RecipeType.PRE_SERVICE_DEPLOYMENT, RecipeType.PRE_CLOUDERA_MANAGER_START)) {
                hostOrchestrator.preServiceDeploymentRecipes(primaryGatewayConfig, allNodes, new StackBasedExitCriteriaModel(stackId));
            } else {
                LOGGER.info("We have no pre-start recipes for this stack");
            }
        } else {
            LOGGER.info("Recipes are empty");
        }
        hostOrchestrator.installFreeIpa(primaryGatewayConfig, gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
    }
}
