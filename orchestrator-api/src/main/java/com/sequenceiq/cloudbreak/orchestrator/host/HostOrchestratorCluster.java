package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;

public class HostOrchestratorCluster {
    private GatewayConfig gatewayConfig;
    private Set<Node> nodes = new HashSet<>();

    public HostOrchestratorCluster(GatewayConfig gatewayConfig, Set<Node> nodes) {
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
