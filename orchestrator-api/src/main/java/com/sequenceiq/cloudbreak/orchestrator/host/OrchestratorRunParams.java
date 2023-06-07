package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public record OrchestratorRunParams(
        Set<Node> nodes,
        List<GatewayConfig> gatewayConfigs,
        String command,
        String errorMessage) {
}
