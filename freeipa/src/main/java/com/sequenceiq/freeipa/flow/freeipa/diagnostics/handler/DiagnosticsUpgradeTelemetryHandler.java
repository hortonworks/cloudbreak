package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.telemetry.upgrade.TelemetryUpgradeService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;

@Component
public class DiagnosticsUpgradeTelemetryHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsUpgradeTelemetryHandler.class);

    @Inject
    private TelemetryUpgradeService telemetryUpgradeService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        LOGGER.debug("Diagnostics cdp-telemetry upgrade started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .exlcudeHosts(parameters.getExcludeHosts())
                .build();
        telemetryUpgradeService.upgradeTelemetryComponent(resourceId, TelemetryComponentType.CDP_TELEMETRY, filter);
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT.selector())
                .withParameters(parameters)
                .build();
    }

    @Override
    public UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType() {
        return UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET;
    }

    @Override
    public String getOperationName() {
        return "Upgrade telemetry";
    }

    @Override
    public String selector() {
        return UPGRADE_DIAGNOSTICS_EVENT.selector();
    }
}
