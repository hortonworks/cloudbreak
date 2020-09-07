package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

public interface ClusterDiagnosticsService {
    void collectDiagnostics(CmDiagnosticsParameters parameters) throws CloudbreakException;
}
