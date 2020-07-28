package com.sequenceiq.datalake.flow.diagnostics;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

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
import com.sequenceiq.datalake.service.sdx.SdxClusterService;
import com.sequenceiq.datalake.service.sdx.diagnostics.DiagnosticsService;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SdxDiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsService.class);

    @Inject
    private DiagnosticsV4Endpoint diagnosticsV4Endpoint;

    @Inject
    private FlowEndpoint flowEndpoint;

    @Inject
    private DiagnosticsParamsConverter diagnosticsParamsConverter;

    @Inject
    private SdxClusterService sdxClusterService;

    public FlowIdentifier startDiagnosticsCollection(Map<String, Object> properties) {
        LOGGER.debug("Start diagnostic collection for SDX");
        DiagnosticsCollectionRequest request = diagnosticsParamsConverter.convertToRequest(properties);
        return diagnosticsV4Endpoint.collectDiagnostics(request);
    }

    public void waitForDiagnosticsCollection(Long sdxId, PollingConfig pollingConfig, FlowIdentifier flowIdentifier) {
        LOGGER.debug("Start polling diagnostics collection for SDX stack id '{}'", sdxId);
        sdxClusterService.findById(sdxId).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        FlowCheckResponse flowCheckResponse = flowEndpoint.hasFlowRunningByFlowId(flowIdentifier.getPollableId());
                        if (!flowCheckResponse.getHasActiveFlow()) {
                            return AttemptResults.justFinish();
                        }
                        return AttemptResults.justContinue();
                    });
        }, () -> {
            throw notFound("SDX cluster", sdxId).get();
        });
    }
}
