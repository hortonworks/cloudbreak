package com.sequenceiq.cloudbreak.telemetry.orchestrator;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;

public interface TelemetryConfigProvider {

    Map<String, SaltPillarProperties> createTelemetryConfigs(Long stackId, Set<TelemetryComponentType> components)
            throws CloudbreakOrchestratorFailedException;
}
