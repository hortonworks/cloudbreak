package com.sequenceiq.cloudbreak.telemetry;

import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;

public interface TelemetryContextProvider<S extends OrchestratorAware> {

    TelemetryContext createTelemetryContext(S stack);
}
