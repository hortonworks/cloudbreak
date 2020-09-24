package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.DiagnosticsTriggerService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.converter.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@AuthorizationResource
public class DiagnosticsV4Controller implements DiagnosticsV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsV4Controller.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService crnService;

    @Inject
    private DiagnosticsTriggerService diagnosticsTriggerService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private ClusterDiagnosticsService clusterDiagnosticsService;

    @Inject
    private VmLogsToVmLogsResponseConverter vmlogsConverter;

    @Override
    @CheckPermissionByResourceObject
    public FlowIdentifier collectDiagnostics(@ResourceObject @Valid DiagnosticsCollectionRequest request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsTriggerService.startDiagnosticsCollection(request, request.getStackCrn(), userCrn);
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        LOGGER.debug("collectDiagnostics called");
        return vmlogsConverter.convert(vmLogsService.getVmLogs());
    }

    @Override
    @CheckPermissionByResourceObject
    public FlowIdentifier collectCmDiagnostics(@ResourceObject @Valid CmDiagnosticsCollectionRequest request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectCMDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsTriggerService.startCmDiagnostics(request, request.getStackCrn(), userCrn);
    }

    @Override
    @DisableCheckPermissions
    public List<String> getCmRoles(String stackCrn) {
        return clusterDiagnosticsService.getClusterComponents(stackCrn);
    }
}
