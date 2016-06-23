package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

import java.util.ArrayList

class DummyContainerOrchestrator : ContainerOrchestrator {
    @Throws(CloudbreakOrchestratorException::class)
    override fun validateApiEndpoint(cred: OrchestrationCredential) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint,
                              exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo> {
        return ArrayList()
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun startContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun stopContainer(info: List<ContainerInfo>, cred: OrchestrationCredential) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun deleteContainer(containerInfos: List<ContainerInfo>, cred: OrchestrationCredential) {
        return
    }

    override fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String>? {
        return null
    }

    override fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return ArrayList()
    }

    override fun name(): String {
        return "DUMMY"
    }

    override fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
    }

    override fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean {
        return true
    }

    override fun getMaxBootstrapNodes(): Int {
        return 0
    }
}
