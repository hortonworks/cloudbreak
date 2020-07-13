package com.sequenceiq.freeipa.controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.freeipa.api.v1.diagnostics.DiagnosticsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.converter.diagnostics.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.freeipa.service.diagnostics.DiagnosticsService;
import com.sequenceiq.freeipa.service.telemetry.VmLogsService;
import com.sequenceiq.freeipa.util.CrnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.validation.Valid;

@Controller
@AuthorizationResource
public class DiagnosticsV1Controller implements DiagnosticsV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsV1Controller.class);

    @Inject
    private CrnService crnService;

    @Inject
    private DiagnosticsService diagnosticsService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private VmLogsToVmLogsResponseConverter vmlogsConverter;

    @Override
    @CheckPermissionByResourceObject
    public void collectDiagnostics(@ResourceObject @Valid DiagnosticsCollectionRequest request) {
        String accountId = crnService.getCurrentAccountId();
        LOGGER.debug("collectDiagnostics called with accountId '{}'", accountId);
        diagnosticsService.startDiagnosticsCollection(request, accountId, crnService.getUserCrn());
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        LOGGER.debug("collectDiagnostics called");
        return vmlogsConverter.convert(vmLogsService.getVmLogs());
    }
}
