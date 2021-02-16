package com.sequenceiq.datalake.service.sdx.diagnostics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.datalake.converter.DiagnosticsParamsConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.validation.diagnostics.DiagnosticsCollectionValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsService.class);

    @Inject
    private DiagnosticsV4Endpoint diagnosticsV4Endpoint;

    @Inject
    private DiagnosticsCollectionValidator diagnosticsCollectionValidator;

    @Inject
    private DiagnosticsParamsConverter diagnosticsParamsConverter;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public FlowIdentifier collectDiagnostics(DiagnosticsCollectionRequest request) {
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByCrn(userId, request.getStackCrn());
        StackV4Response stackV4Response = sdxService.getDetail(cluster.getClusterName(), new HashSet<>(), cluster.getAccountId());
        diagnosticsCollectionValidator.validate(request, stackV4Response);
        Map<String, Object> properties = diagnosticsParamsConverter.convertFromRequest(request);
        SdxDiagnosticsCollectionEvent event = new SdxDiagnosticsCollectionEvent(cluster.getId(), userId, properties, null);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDiagnosticsCollection(event);
        LOGGER.debug("Start diagnostics collection with flow pollable identifier: {}", flowIdentifier.getPollableId());
        return flowIdentifier;
    }

    public FlowIdentifier collectCmDiagnostics(CmDiagnosticsCollectionRequest request) {
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByCrn(userId, request.getStackCrn());
        StackV4Response stackV4Response = sdxService.getDetail(cluster.getClusterName(), new HashSet<>(), cluster.getAccountId());
        diagnosticsCollectionValidator.validate(request, stackV4Response);
        Map<String, Object> properties = diagnosticsParamsConverter.convertFromCmRequest(request);
        SdxCmDiagnosticsCollectionEvent event = new SdxCmDiagnosticsCollectionEvent(cluster.getId(), userId, properties, null);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerCmDiagnosticsCollection(event);
        LOGGER.debug("Start CM based diagnostics collection with flow pollable identifier: {}", flowIdentifier.getPollableId());
        return flowIdentifier;
    }

    public VmLogsResponse getVmLogs() {
        return diagnosticsV4Endpoint.getVmLogs();
    }

    public List<String> getCmRoles(String stackCrn) {
        return diagnosticsV4Endpoint.getCmRoles(stackCrn);
    }

    public ListDiagnosticsCollectionResponse getDiagnosticsCollections(String crn) {
        return diagnosticsV4Endpoint.listCollections(crn);
    }
}
