package com.sequenceiq.cloudbreak.core.bootstrap.service.host

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostOrchestratorClusterContext
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

@Component
class HostClusterAvailabilityCheckerTask : StackBasedStatusCheckerTask<HostOrchestratorClusterContext>() {

    override fun checkStatus(context: HostOrchestratorClusterContext): Boolean {
        val missingNodes = context.hostOrchestrator.getMissingNodes(context.gatewayConfig, context.nodes)
        LOGGER.debug("Missing nodes from orchestrator cluster: {}", missingNodes)
        return missingNodes.isEmpty()
    }

    override fun handleTimeout(t: HostOrchestratorClusterContext) {
        return
    }

    override fun successMessage(t: HostOrchestratorClusterContext): String {
        return "Host orchestration API is available and the agents are registered."
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(HostClusterAvailabilityCheckerTask::class.java)
    }
}
