package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.converter.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.diagnostics.DiagnosticsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.service.diagnostics.DiagnosticsTriggerService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class DiagnosticsV1Controller implements DiagnosticsV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsV1Controller.class);

    @Inject
    private CrnService crnService;

    @Inject
    private DiagnosticsTriggerService diagnosticsTriggerService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private VmLogsToVmLogsResponseConverter vmlogsConverter;

    @Override
    @CheckPermissionByResourceObject
    public FlowIdentifier collectDiagnostics(@ResourceObject @Valid DiagnosticsCollectionRequest request) {
        String accountId = crnService.getCurrentAccountId();
        LOGGER.debug("collectDiagnostics called with accountId '{}'", accountId);
        return diagnosticsTriggerService.startDiagnosticsCollection(request, accountId, crnService.getUserCrn());
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        LOGGER.debug("collectDiagnostics called");
        return vmlogsConverter.convert(vmLogsService.getVmLogs());
    }
}
