package com.sequenceiq.datalake.controller.diagnostics;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.datalake.service.sdx.diagnostics.DiagnosticsService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.DiagnosticsEndpoint;

@Controller
@AuthorizationResource
public class DiagnosticsController implements DiagnosticsEndpoint {

    @Inject
    private DiagnosticsService diagnosticsService;

    @Override
    @CheckPermissionByResourceObject
    public FlowIdentifier collectDiagnostics(@ResourceObject @Valid DiagnosticsCollectionRequest request) {
        return diagnosticsService.collectDiagnostics(request);
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        return diagnosticsService.getVmLogs();
    }
}
