package com.sequenceiq.cloudbreak.core.bootstrap.service.host.context

import java.util.HashSet

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.service.StackContext

class HostOrchestratorClusterContext(stack: Stack, val hostOrchestrator: HostOrchestrator, val gatewayConfig: GatewayConfig, nodes: Set<Node>) : StackContext(stack) {
    val nodes: Set<Node> = HashSet()

    init {
        this.nodes = nodes
    }
}
