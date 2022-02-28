package com.sequenceiq.cloudbreak.orchestrator.container;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public class ContainerOrchestratorCluster {

    private final GatewayConfig gatewayConfig;

    private final Set<Node> nodes;

    public ContainerOrchestratorCluster(GatewayConfig gatewayConfig, Set<Node> nodes) {
        this.gatewayConfig = gatewayConfig;
        this.nodes = nodes;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}
