package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.service.StackContext;

public class DaemonContext extends StackContext {

    private String gatewayAddress;
    private ContainerOrchestrator containerOrchestrator;

    public DaemonContext(Stack stack, String gatewayAddress, ContainerOrchestrator containerOrchestrator) {
        super(stack);
        this.gatewayAddress = gatewayAddress;
        this.containerOrchestrator = containerOrchestrator;
    }

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public ContainerOrchestrator getContainerOrchestrator() {
        return containerOrchestrator;
    }
}
