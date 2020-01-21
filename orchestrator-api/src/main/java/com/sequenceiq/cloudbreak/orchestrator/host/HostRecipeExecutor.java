package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostRecipeExecutor {

    void uploadRecipes(List<GatewayConfig> allGatewayConfigs, Map<String, List<RecipeModel>> recipes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException;

    void preCluserManagerStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
        throws CloudbreakOrchestratorFailedException;

    void postClusterManagerStartRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
        throws CloudbreakOrchestratorFailedException;

    void preTerminationRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel, boolean forced)
            throws CloudbreakOrchestratorFailedException;

    void postInstallRecipes(GatewayConfig gatewayConfig, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException;

}
