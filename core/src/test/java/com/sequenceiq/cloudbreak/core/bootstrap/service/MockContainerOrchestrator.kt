package com.sequenceiq.cloudbreak.core.bootstrap.service

import java.util.ArrayList

import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

open class MockContainerOrchestrator : ContainerOrchestrator {

    override fun name(): String {
        return "mock"
    }

    override fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria) {
        return
    }

    @Throws(CloudbreakOrchestratorCancelledException::class, CloudbreakOrchestratorFailedException::class)
    override fun bootstrap(gatewayConfig: GatewayConfig, config: ContainerConfig, nodes: Set<Node>, consulServerCount: Int, exitCriteriaModel: ExitCriteriaModel) {
        return
    }

    @Throws(CloudbreakOrchestratorCancelledException::class, CloudbreakOrchestratorFailedException::class)
    override fun bootstrapNewNodes(gatewayConfig: GatewayConfig, containerConfig: ContainerConfig, nodes: Set<Node>, exitCriteriaModel: ExitCriteriaModel) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun validateApiEndpoint(cred: OrchestrationCredential) {
        return
    }

    @Throws(CloudbreakOrchestratorException::class)
    override fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint,
                              exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo>? {
        return null
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

    override fun getMissingNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return ArrayList()
    }

    override fun getAvailableNodes(gatewayConfig: GatewayConfig, nodes: Set<Node>): List<String> {
        return ArrayList()
    }

    override fun isBootstrapApiAvailable(gatewayConfig: GatewayConfig): Boolean {
        return false
    }

    override fun getMaxBootstrapNodes(): Int {
        return 100
    }
}
