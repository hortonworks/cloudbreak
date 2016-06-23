package com.sequenceiq.cloudbreak.orchestrator.container

import java.util.HashSet

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.orchestrator.model.Node

class ContainerOrchestratorCluster(val gatewayConfig: GatewayConfig, nodes: Set<Node>) {
    val nodes: Set<Node> = HashSet()

    init {
        this.nodes = nodes
    }
}
