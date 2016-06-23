package com.sequenceiq.cloudbreak.orchestrator.container

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

interface ContainerOrchestrator : ContainerOrchestrationBootstrap {

    @Throws(CloudbreakOrchestratorException::class)
    fun validateApiEndpoint(cred: OrchestrationCredential)

    @Throws(CloudbreakOrchestratorException::class)
    fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint, exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo>

    @Throws(CloudbreakOrchestratorException::class)
    fun startContainer(info: List<ContainerInfo>, cred: OrchestrationCredential)

    @Throws(CloudbreakOrchestratorException::class)
    fun stopContainer(info: List<ContainerInfo>, cred: OrchestrationCredential)

    @Throws(CloudbreakOrchestratorException::class)
    fun deleteContainer(containerInfos: List<ContainerInfo>, cred: OrchestrationCredential)

    fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String>

    fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String>

}
