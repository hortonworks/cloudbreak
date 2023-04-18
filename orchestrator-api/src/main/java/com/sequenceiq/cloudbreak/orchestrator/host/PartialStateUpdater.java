package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface PartialStateUpdater {
    void updatePartialSaltDefinition(byte[] partialSaltDefinition, List<String> components, List<GatewayConfig> gatewayConfigs, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;
}
