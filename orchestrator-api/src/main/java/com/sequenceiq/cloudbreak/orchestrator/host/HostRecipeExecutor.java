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

    void preInstallRecipes(GatewayConfig gatewayConfig, Map<String, List<RecipeModel>> recipes, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorFailedException;

}
