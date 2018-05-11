package com.sequenceiq.cloudbreak.core.bootstrap.service.container.context;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrationBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ContainerBootstrapApiContext extends StackContext {

    private final GatewayConfig gatewayConfig;

    private final ContainerOrchestrator containerOrchestrator;

    public ContainerBootstrapApiContext(Stack stack, GatewayConfig gatewayConfig, ContainerOrchestrator containerOrchestrator) {
        super(stack);
        this.gatewayConfig = gatewayConfig;
        this.containerOrchestrator = containerOrchestrator;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public ContainerOrchestrationBootstrap getContainerOrchestrator() {
        return containerOrchestrator;
    }
}
