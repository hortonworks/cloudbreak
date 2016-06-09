package com.sequenceiq.cloudbreak.core.bootstrap.service.host.context;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.StackContext;

public class HostOrchestratorClusterContext extends StackContext {

    private HostOrchestrator hostOrchestrator;
    private GatewayConfig gatewayConfig;
    private Set<Node> nodes = new HashSet<>();

    public HostOrchestratorClusterContext(Stack stack, HostOrchestrator hostOrchestrator, GatewayConfig gatewayConfig, Set<Node> nodes) {
        super(stack);
        this.hostOrchestrator = hostOrchestrator;
        this.gatewayConfig = gatewayConfig;
        this.nodes = nodes;
    }

    public HostOrchestrator getHostOrchestrator() {
        return hostOrchestrator;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}
