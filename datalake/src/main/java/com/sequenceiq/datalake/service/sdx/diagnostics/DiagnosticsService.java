package com.sequenceiq.datalake.service.sdx.diagnostics;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsCollectionEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
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
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public FlowIdentifier collectDiagnostics(DiagnosticsCollectionRequest request) {
        String userId = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster cluster = sdxService.getByCrn(userId, request.getStackCrn());
        StackV4Response stackV4Response = sdxService.getDetail(cluster.getClusterName(), new HashSet<>());
        diagnosticsCollectionValidator.validate(request, stackV4Response);
        Map<String, Object> properties = diagnosticsParamsConverter.convertFromRequest(request);
        SdxDiagnosticsCollectionEvent event = new SdxDiagnosticsCollectionEvent(cluster.getId(), userId, properties);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDiagnosticsCollection(event);
        LOGGER.debug("Start diagnostics collection with flow pollable identifier: {}", flowIdentifier.getPollableId());
        return flowIdentifier;
    }

    public void startDiagnosticsCollection(Map<String, Object> properties) {
        LOGGER.debug("Start diagnostic collection for SDX");
        DiagnosticsCollectionRequest request = diagnosticsParamsConverter.convertToRequest(properties);
        //diagnosticsV4Endpoint.collectDiagnostics(request);
    }

    public void waitForDiagnosticsCollection(Long sdxId, PollingConfig pollingConfig, Map<String, Object> properties) {
        LOGGER.debug("Start polling diagnostics collection for SDX stack id '{}'", sdxId);
        sdxClusterRepository.findById(sdxId).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.debug("Polling diagnostics collection through stack API");
                        return AttemptResults.finishWith(null);
                    });
        }, () -> {
            throw notFound("SDX cluster", sdxId).get();
        });
    }

    public VmLogsResponse getVmLogs() {
        return diagnosticsV4Endpoint.getVmLogs();
    }

}
