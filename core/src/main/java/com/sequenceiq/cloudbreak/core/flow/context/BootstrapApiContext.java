package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.service.StackContext;

public class BootstrapApiContext extends StackContext {

    private GatewayConfig gatewayConfig;
    private ContainerOrchestrator containerOrchestrator;

    public BootstrapApiContext(Stack stack, GatewayConfig gatewayConfig, ContainerOrchestrator containerOrchestrator) {
        super(stack);
        this.gatewayConfig = gatewayConfig;
        this.containerOrchestrator = containerOrchestrator;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public ContainerOrchestrator getContainerOrchestrator() {
        return containerOrchestrator;
    }
}
