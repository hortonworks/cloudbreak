package com.sequenceiq.datalake.flow.diagnostics;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.datalake.converter.DiagnosticsParamsConverter;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;

@Service
public class SdxDiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDiagnosticsFlowService.class);

    private static final String DIAGNOSTICS_COLLECTION_FAILED_STATE = "DIAGNOSTICS_COLLECTION_FAILED_STATE";

    @Inject
    private DiagnosticsV4Endpoint diagnosticsV4Endpoint;

    @Inject
    private FlowEndpoint flowEndpoint;

    @Inject
    private DiagnosticsParamsConverter diagnosticsParamsConverter;

    @Inject
    private SdxService sdxService;

    public FlowIdentifier startDiagnosticsCollection(Map<String, Object> properties) {
        LOGGER.debug("Start diagnostic collection for SDX");
        DiagnosticsCollectionRequest request = diagnosticsParamsConverter.convertToRequest(properties);
        return diagnosticsV4Endpoint.collectDiagnostics(request);
    }

    public void waitForDiagnosticsCollection(Long sdxId, PollingConfig pollingConfig, FlowIdentifier flowIdentifier) {
        LOGGER.debug("Start polling diagnostics collection for SDX stack id '{}'", sdxId);
        sdxService.getById(sdxId);
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    List<FlowLogResponse> flowLogs = flowEndpoint.getFlowLogsByFlowId(flowIdentifier.getPollableId());
                    if (hasFlowFailed(flowLogs)) {
                        return AttemptResults.breakFor("Diagnostic collection flow failed in Cloudbreak.");
                    }
                    if (!flowLogs.isEmpty() && flowLogs.get(0).getFinalized()) {
                        return AttemptResults.justFinish();
                    }
                    return AttemptResults.justContinue();
                });
    }

    private boolean hasFlowFailed(List<FlowLogResponse> flowLogs) {
        return flowLogs.stream().map(FlowLogResponse::getCurrentState).anyMatch(DIAGNOSTICS_COLLECTION_FAILED_STATE::equals);
    }
}
