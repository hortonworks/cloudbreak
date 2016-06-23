package com.sequenceiq.cloudbreak.orchestrator.host

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.RecipeModel
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

interface HostRecipeExecutor {

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun uploadRecipes(gatewayConfig: GatewayConfig, recipes: Map<String, List<RecipeModel>>, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel)

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun preInstallRecipes(gatewayConfig: GatewayConfig, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel)

    @Throws(CloudbreakOrchestratorFailedException::class)
    fun postInstallRecipes(gatewayConfig: GatewayConfig, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel)

}
