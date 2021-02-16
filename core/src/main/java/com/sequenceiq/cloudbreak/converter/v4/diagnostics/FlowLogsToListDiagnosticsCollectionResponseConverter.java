package com.sequenceiq.cloudbreak.converter.v4.diagnostics;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_COLLECTION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsCollectionsState.DIAGNOSTICS_COLLECTION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.config.DiagnosticsCollectionFlowConfig;
import com.sequenceiq.cloudbreak.telemetry.converter.FlowPayloadToDiagnosticDetailsConverter;
import com.sequenceiq.common.api.diagnostics.DiagnosticsCollection;
import com.sequenceiq.common.api.diagnostics.DiagnosticsCollectionStatus;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@Component
public class FlowLogsToListDiagnosticsCollectionResponseConverter {

    @Inject
    private FlowPayloadToDiagnosticDetailsConverter flowPayloadToDiagnosticDetailsConverter;

    @Inject
    private DiagnosticsCollectionFlowConfig diagnosticsCollectionFlowConfig;

    public ListDiagnosticsCollectionResponse convert(List<FlowLog> flowLogs) {
        ListDiagnosticsCollectionResponse response = new ListDiagnosticsCollectionResponse();
        if (!flowLogs.isEmpty()) {
            List<DiagnosticsCollection> collections = flowLogs.stream()
                    .map(flowLog -> {
                        DiagnosticsCollection collection = new DiagnosticsCollection();
                        collection.setFlowId(flowLog.getFlowId());
                        collection.setCreated(flowLog.getCreated());
                        collection.setProperties(flowPayloadToDiagnosticDetailsConverter.convert(flowLog.getPayload()));
                        collection.setStatus(calculateStatus(flowLog));
                        collection.setCurrentFlowStatus(flowLog.getCurrentState());
                        collection.setProgressPercentage(calculateProgressPercentage(flowLog));
                        return collection;
                    }).collect(Collectors.toList());
            response.setCollections(collections);
        }
        return response;
    }

    private int calculateProgressPercentage(FlowLog flowLog) {
        return flowPayloadToDiagnosticDetailsConverter.calculateProgressPercentage(flowLog.getFinalized(),
                StateStatus.FAILED.equals(flowLog.getStateStatus()),
                () -> diagnosticsCollectionFlowConfig.getProgressPercentageForState(flowLog.getCurrentState()));
    }

    private DiagnosticsCollectionStatus calculateStatus(FlowLog flowLog) {
        return flowPayloadToDiagnosticDetailsConverter.calculateStatus(flowLog.getCurrentState(),
                DIAGNOSTICS_COLLECTION_FINISHED_STATE.name(), DIAGNOSTICS_COLLECTION_FAILED_STATE.name(),
                HANDLED_FAILED_DIAGNOSTICS_COLLECTION_EVENT.name(), flowLog.getNextEvent(), flowLog.getFinalized(),
                StateStatus.FAILED.equals(flowLog.getStateStatus()));
    }
}
