package com.sequenceiq.cloudbreak.orchestrator;

import java.util.HashSet;
import java.util.Set;

public class ContainerOrchestratorCluster {
    private GatewayConfig gatewayConfig;
    private Set<Node> nodes = new HashSet<>();

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
