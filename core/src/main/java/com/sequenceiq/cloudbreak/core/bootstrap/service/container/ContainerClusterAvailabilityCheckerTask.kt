package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

@Component
class ContainerClusterAvailabilityCheckerTask : StackBasedStatusCheckerTask<ContainerOrchestratorClusterContext>() {

    override fun checkStatus(context: ContainerOrchestratorClusterContext): Boolean {
        val missingNodes = context.containerOrchestrator.getMissingNodes(context.gatewayConfig, context.nodes)
        LOGGER.debug("Missing nodes from orchestrator cluster: {}", missingNodes)
        return missingNodes.isEmpty()
    }

    override fun handleTimeout(t: ContainerOrchestratorClusterContext) {
        return
    }

    override fun successMessage(t: ContainerOrchestratorClusterContext): String {
        return "Container orchestration API is available and the agents are registered."
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ContainerClusterAvailabilityCheckerTask::class.java)
    }
}
