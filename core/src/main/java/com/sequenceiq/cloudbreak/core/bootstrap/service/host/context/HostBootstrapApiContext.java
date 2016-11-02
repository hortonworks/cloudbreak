package com.sequenceiq.cloudbreak.core.bootstrap.service.host.context;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.StackContext;

public class HostBootstrapApiContext extends StackContext {

    private GatewayConfig gatewayConfig;

    private HostOrchestrator hostOrchestrator;

    public HostBootstrapApiContext(Stack stack, GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator) {
        super(stack);
        this.gatewayConfig = gatewayConfig;
        this.hostOrchestrator = hostOrchestrator;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public HostOrchestrator getHostOrchestrator() {
        return hostOrchestrator;
    }
}
