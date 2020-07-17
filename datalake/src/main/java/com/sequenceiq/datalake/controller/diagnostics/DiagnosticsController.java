package com.sequenceiq.datalake.controller.diagnostics;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.sdx.api.endpoint.DiagnosticsEndpoint;

@Controller
@AuthorizationResource
public class DiagnosticsController implements DiagnosticsEndpoint {

    @Inject
    private DiagnosticsV4Endpoint diagnosticsV4Endpoint;

    @Override
    public void collectDiagnostics(@ResourceObject @Valid DiagnosticsCollectionRequest request) {
        diagnosticsV4Endpoint.collectDiagnostics(request);
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        return diagnosticsV4Endpoint.getVmLogs();
    }
}
