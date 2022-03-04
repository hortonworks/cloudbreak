package com.sequenceiq.cloudbreak.telemetry.diagnostics;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;

@FunctionalInterface
public interface DiagnosticsOperationFunction {

    void apply(OrchestratorMetadata orchestratorMetadata, DiagnosticParameters parameters) throws CloudbreakOrchestratorFailedException;

}
