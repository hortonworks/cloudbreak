package com.sequenceiq.cloudbreak.core.bootstrap.service.container.context

import java.util.HashSet

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node
import com.sequenceiq.cloudbreak.service.StackContext

class ContainerOrchestratorClusterContext(stack: Stack, val containerOrchestrator: ContainerOrchestrator, val gatewayConfig: GatewayConfig, nodes: Set<Node>) : StackContext(stack) {
    val nodes: Set<Node> = HashSet()

    init {
        this.nodes = nodes
    }
}
