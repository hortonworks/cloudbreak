package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.telemetry.TelemetryComponentType;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataFilter;
import com.sequenceiq.cloudbreak.telemetry.upgrade.TelemetryUpgradeService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DiagnosticsUpgradeTelemetryHandler extends AbstractDiagnosticsOperationHandler {

    @Inject
    private TelemetryUpgradeService telemetryUpgradeService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        OrchestratorMetadataFilter filter = OrchestratorMetadataFilter.Builder.newBuilder()
                .includeHosts(parameters.getHosts())
                .includeHostGroups(parameters.getHostGroups())
                .exlcudeHosts(parameters.getExcludeHosts())
                .build();
        telemetryUpgradeService.upgradeTelemetryComponent(resourceId, TelemetryComponentType.CDP_TELEMETRY, filter);
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT.selector())
                .withParameters(parameters)
                .withHosts(parameters.getHosts())
                .withHostGroups(parameters.getHostGroups())
                .withExcludeHosts(parameters.getExcludeHosts())
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
