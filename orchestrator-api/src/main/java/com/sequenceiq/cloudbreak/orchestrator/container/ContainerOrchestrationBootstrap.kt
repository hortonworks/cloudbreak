package com.sequenceiq.cloudbreak.orchestrator.container

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

interface ContainerOrchestrationBootstrap {

    fun name(): String

    fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria)

    @Throws(CloudbreakOrchestratorException::class)
    fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int,
                  exitCriteriaModel: ExitCriteriaModel)

    @Throws(CloudbreakOrchestratorException::class)
    fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>,
                          exitCriteriaModel: ExitCriteriaModel)

    fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean

    val maxBootstrapNodes: Int
}
