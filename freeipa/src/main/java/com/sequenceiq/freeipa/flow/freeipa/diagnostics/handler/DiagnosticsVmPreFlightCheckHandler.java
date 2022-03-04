package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.VM_PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.telemetry.diagnostics.DiagnosticsOperationsService;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;

@Component
public class DiagnosticsVmPreFlightCheckHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsVmPreFlightCheckHandler.class);

    @Inject
    private DiagnosticsOperationsService diagnosticsOperationsService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        LOGGER.debug("Diagnostics collection VM preflight check started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
        if (!DiagnosticsDestination.ENG.equals(parameters.getDestination())) {
            diagnosticsOperationsService.vmPreflightCheck(resourceId, parameters);
        } else {
            LOGGER.debug("Diagnostics VM preflight check skipped.");
        }
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT.selector())
                .withParameters(parameters)
                .build();
    }

    @Override
    public UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType() {
        return UsageProto.CDPVMDiagnosticsFailureType.Value.PREFLIGHT_VM_CHECK_FAILURE;
    }

    @Override
    public String getOperationName() {
        return "VM Pre-flight check";
    }

    @Override
    public String selector() {
        return VM_PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector();
    }
}
