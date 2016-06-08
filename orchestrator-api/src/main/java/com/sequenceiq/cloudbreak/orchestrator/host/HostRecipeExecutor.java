package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel;

public interface HostRecipeExecutor {

    void preInstallRecipes(GatewayConfig gatewayConfig, Map<String, List<RecipeModel>> recipes);

}
