package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.service.diagnostics.DiagnosticsService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.converter.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class DiagnosticsV4Controller implements DiagnosticsV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsV4Controller.class);

    @Inject
    private CloudbreakRestRequestThreadLocalService crnService;

    @Inject
    private DiagnosticsService diagnosticsService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private ClusterDiagnosticsService clusterDiagnosticsService;

    @Inject
    private VmLogsToVmLogsResponseConverter vmlogsConverter;

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATALAKE)
    public FlowIdentifier collectDiagnostics(@RequestObject DiagnosticsCollectionRequest request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsService.startDiagnosticsCollection(request, request.getStackCrn(), userCrn, request.getUuid());
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        return vmlogsConverter.convert(vmLogsService.getVmLogs());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public ListDiagnosticsCollectionResponse listCollections(@ResourceCrn String crn) {
        return diagnosticsService.getDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public void cancelCollections(@ResourceCrn String crn) {
        diagnosticsService.cancelDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATALAKE)
    public FlowIdentifier collectCmDiagnostics(@RequestObject CmDiagnosticsCollectionRequest request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectCMDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsService.startCmDiagnostics(request, request.getStackCrn(), userCrn);
    }

    @Override
    @InternalOnly
    public List<String> getCmRoles(@ResourceCrn String stackCrn) {
        return clusterDiagnosticsService.getClusterComponents(stackCrn);
    }
}
