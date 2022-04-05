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
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
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

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FlowIdentifier startDiagnosticsCollection(Map<String, Object> properties) {
        LOGGER.debug("Start diagnostic collection for SDX");
        DiagnosticsCollectionRequest request = diagnosticsParamsConverter.convertToRequest(properties);
        return diagnosticsV4Endpoint.collectDiagnostics(request);
    }

    public FlowIdentifier startCmDiagnosticsCollection(Map<String, Object> properties) {
        LOGGER.debug("Start CM based diagnostic collection for SDX");
        CmDiagnosticsCollectionRequest request = diagnosticsParamsConverter.convertToCmRequest(properties);
        return diagnosticsV4Endpoint.collectCmDiagnostics(request);
    }

    public void waitForDiagnosticsCollection(Long sdxId, PollingConfig pollingConfig, FlowIdentifier flowIdentifier) {
        waitForDiagnosticsCollection(sdxId, pollingConfig, flowIdentifier, false);
    }

    public void waitForDiagnosticsCollection(Long sdxId, PollingConfig pollingConfig, FlowIdentifier flowIdentifier, boolean cmBundle) {
        String startPollingMessage = cmBundle ? String.format("Start polling CM based diagnostics collection for SDX stack id '%s'", sdxId)
                : String.format("Start polling diagnostics collection for SDX stack id '%s'", sdxId);
        String failedMessage = cmBundle ? "Cm based diagnostic collection flow failed in Cloudbreak."
                : "Diagnostic collection flow failed in Cloudbreak.";
        LOGGER.debug(startPollingMessage);
        sdxService.getById(sdxId);
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    List<FlowLogResponse> flowLogs = ThreadBasedUserCrnProvider.doAsInternalActor(
                            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            () -> flowEndpoint.getFlowLogsByFlowId(flowIdentifier.getPollableId()));
                    if (hasFlowFailed(flowLogs)) {
                        return AttemptResults.breakFor(failedMessage);
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
