package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ContainerOrchestratorClusterContext extends StackContext {

    private ContainerOrchestrator containerOrchestrator;
    private GatewayConfig gatewayConfig;
    private Set<Node> nodes = new HashSet<>();

    public ContainerOrchestratorClusterContext(Stack stack, ContainerOrchestrator containerOrchestrator, GatewayConfig gatewayConfig, Set<Node> nodes) {
        super(stack);
        this.containerOrchestrator = containerOrchestrator;
        this.gatewayConfig = gatewayConfig;
        this.nodes = nodes;
    }

    public ContainerOrchestrator getContainerOrchestrator() {
        return containerOrchestrator;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}
