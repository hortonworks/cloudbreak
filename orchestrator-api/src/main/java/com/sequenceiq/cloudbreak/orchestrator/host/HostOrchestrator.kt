package com.sequenceiq.cloudbreak.orchestrator.host

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

interface HostOrchestrator : HostRecipeExecutor {

    fun name(): String

    fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria)

    @Throws(CloudbreakOrchestratorException::class)
    fun bootstrap(gatewayConfig: GatewayConfig, targets: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel)

    @Throws(CloudbreakOrchestratorException::class)
    fun bootstrapNewNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel)

    fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean

    val maxBootstrapNodes: Int

    @Throws(CloudbreakOrchestratorException::class)
    fun runService(gatewayConfig: GatewayConfig, allNodes: Set<Node>, pillarConfig: SaltPillarConfig, exitCriteriaModel: ExitCriteriaModel)

    @Throws(CloudbreakOrchestratorException::class)
    fun resetAmbari(gatewayConfig: GatewayConfig, target: Set<String>, allNodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel)

    fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String>

    fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String>

    @Throws(CloudbreakOrchestratorException::class)
    fun tearDown(gatewayConfig: GatewayConfig, hostnames: List<String>)
}
