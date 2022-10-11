package com.sequenceiq.cloudbreak.core.bootstrap.service.host.context;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.StackContext;
import com.sequenceiq.common.api.type.Tunnel;

public class HostBootstrapApiContext extends StackContext {

    private final GatewayConfig gatewayConfig;

    private final HostOrchestrator hostOrchestrator;

    private final Tunnel tunnel;

    public HostBootstrapApiContext(StackDtoDelegate stack, GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator, Tunnel tunnel) {
        super(stack);
        this.gatewayConfig = gatewayConfig;
        this.hostOrchestrator = hostOrchestrator;
        this.tunnel = tunnel;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public HostOrchestrator getHostOrchestrator() {
        return hostOrchestrator;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }
}
