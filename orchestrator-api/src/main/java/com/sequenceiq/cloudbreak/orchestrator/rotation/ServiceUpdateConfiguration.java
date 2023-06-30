package com.sequenceiq.cloudbreak.orchestrator.rotation;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public record ServiceUpdateConfiguration(
        GatewayConfig primaryGatewayConfig,
        String oldSaltBootPassword,
        String newSaltBootPassword,
        String oldSaltBootPrivateKey,
        String newSaltBootPrivateKey,
        String configFolder,
        String configFile,
        String newConfig,
        String oldConfig,
        Set<String> targetPrivateIps,
        Set<String> targetFqdns,
        List<String> serviceRestartActions,
        int maxRetryCount,
        ExitCriteriaModel exitCriteriaModel) {
}
