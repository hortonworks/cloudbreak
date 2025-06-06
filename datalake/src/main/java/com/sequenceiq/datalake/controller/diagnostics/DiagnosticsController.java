package com.sequenceiq.datalake.controller.diagnostics;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.datalake.service.sdx.diagnostics.DiagnosticsService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.DiagnosticsEndpoint;

@Controller
public class DiagnosticsController implements DiagnosticsEndpoint {

    @Inject
    private DiagnosticsService diagnosticsService;

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATALAKE)
    public FlowIdentifier collectDiagnostics(@RequestObject DiagnosticsCollectionRequest request) {
        return diagnosticsService.collectDiagnostics(request);
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        return diagnosticsService.getVmLogs();
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public ListDiagnosticsCollectionResponse listCollections(@ResourceCrn String crn) {
        return diagnosticsService.getDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public void cancelCollections(@ResourceCrn String crn) {
        diagnosticsService.cancelDiagnosticsCollection(crn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATALAKE)
    public FlowIdentifier collectCmDiagnostics(@RequestObject CmDiagnosticsCollectionRequest request) {
        return diagnosticsService.collectCmDiagnostics(request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public List<String> getCmRoles(@ResourceCrn String stackCrn) {
        return diagnosticsService.getCmRoles(stackCrn);
    }
}
