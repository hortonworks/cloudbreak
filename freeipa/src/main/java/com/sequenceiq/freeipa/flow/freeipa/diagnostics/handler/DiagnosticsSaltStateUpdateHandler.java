package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.SALT_STATE_UPDATE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.telemetry.upgrade.TelemetryUpgradeService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;

@Component
public class DiagnosticsSaltStateUpdateHandler extends AbstractDiagnosticsOperationHandler {

    @Inject
    private TelemetryUpgradeService telemetryUpgradeService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        telemetryUpgradeService.upgradeTelemetrySaltStates(resourceId, Set.of(TelemetryComponentType.CDP_TELEMETRY));
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT.selector())
                .withParameters(parameters)
                .build();
    }

    @Override
    public UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType() {
        return UsageProto.CDPVMDiagnosticsFailureType.Value.UNSET;
    }

    @Override
    public String getOperationName() {
        return "Salt state update";
    }

    @Override
    public String selector() {
        return SALT_STATE_UPDATE_DIAGNOSTICS_EVENT.selector();
    }
}
