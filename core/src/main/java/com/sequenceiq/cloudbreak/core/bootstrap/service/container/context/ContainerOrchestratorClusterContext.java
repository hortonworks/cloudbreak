package com.sequenceiq.cloudbreak.core.bootstrap.service.container.context;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ContainerOrchestratorClusterContext extends StackContext {

    private final ContainerOrchestrator containerOrchestrator;

    private final GatewayConfig gatewayConfig;

    private final Set<Node> nodes;

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
