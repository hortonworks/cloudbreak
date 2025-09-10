package com.sequenceiq.datalake.service.sdx.diagnostics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.datalake.converter.DiagnosticsParamsConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsFlowConfig;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StackService;
import com.sequenceiq.datalake.service.validation.diagnostics.DiagnosticsCollectionValidator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

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

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private StackService stackService;

    public FlowIdentifier collectDiagnostics(DiagnosticsCollectionRequest request) {
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByCrn(userId, request.getStackCrn());
        StackV4Response stackV4Response = stackService.getDetail(cluster.getClusterName(), new HashSet<>(), cluster.getAccountId());
        diagnosticsCollectionValidator.validate(request, stackV4Response);
        Map<String, Object> properties = diagnosticsParamsConverter.convertFromRequest(request);
        SdxDiagnosticsCollectionEvent event = new SdxDiagnosticsCollectionEvent(cluster.getId(), userId, properties, null);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDiagnosticsCollection(event, cluster.getClusterName());
        LOGGER.debug("Start diagnostics collection with flow pollable identifier: {}", flowIdentifier.getPollableId());
        return flowIdentifier;
    }

    public void cancelDiagnosticsCollection(String stackCrn) {
        diagnosticsV4Endpoint.cancelCollections(stackCrn);
        List<FlowLog> flowLogs = flowLogDBService.getLatestNotFinishedFlowLogsByCrnAndType(stackCrn, ClassValue.of(SdxDiagnosticsFlowConfig.class));
        flowLogs.stream()
                .filter(f -> !f.getFinalized())
                .forEach(cancelFlow());
    }

    private Consumer<FlowLog> cancelFlow() {
        return f -> {
            try {
                flow2Handler.cancelFlow(f.getResourceId(), f.getFlowId());
            } catch (TransactionService.TransactionExecutionException e) {
                String errorMessage = String.format("Transaction error during cancelling diagnostics flow [stack_id: %s] [flow_id: %s]",
                        f.getResourceId(), f.getFlowId());
                LOGGER.debug(errorMessage, e);
                throw new CloudbreakServiceException(errorMessage);
            }
        };
    }

    public FlowIdentifier collectCmDiagnostics(CmDiagnosticsCollectionRequest request) {
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByCrn(userId, request.getStackCrn());
        StackV4Response stackV4Response = stackService.getDetail(cluster.getClusterName(), new HashSet<>(), cluster.getAccountId());
        diagnosticsCollectionValidator.validate(request, stackV4Response);
        Map<String, Object> properties = diagnosticsParamsConverter.convertFromCmRequest(request);
        SdxCmDiagnosticsCollectionEvent event = new SdxCmDiagnosticsCollectionEvent(cluster.getId(), userId, properties, null);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerCmDiagnosticsCollection(event, cluster.getClusterName());
        LOGGER.debug("Start CM based diagnostics collection with flow pollable identifier: {}", flowIdentifier.getPollableId());
        return flowIdentifier;
    }

    public VmLogsResponse getVmLogs() {
        return diagnosticsV4Endpoint.getVmLogs();
    }

    public List<String> getCmRoles(String stackCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> diagnosticsV4Endpoint.getCmRoles(stackCrn));
    }

    public ListDiagnosticsCollectionResponse getDiagnosticsCollections(String crn) {
        return diagnosticsV4Endpoint.listCollections(crn);
    }
}
