package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.INIT_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_UPGRADE_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.telemetry.diagnostics.DiagnosticsOperationsService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;

@Component
public class DiagnosticsInitHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsInitHandler.class);

    @Inject
    private DiagnosticsOperationsService diagnosticsOperationsService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        parameters.setUuid(event.getEvent().getHeaders().get(FlowConstants.FLOW_ID));
        Map<String, Object> parameterMap = parameters.toMap();
        LOGGER.debug("Diagnostics collection initialization started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
        diagnosticsOperationsService.init(resourceId, parameters);
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_UPGRADE_EVENT.selector())
                .withParameters(parameters)
                .build();
    }

    @Override
    public UsageProto.CDPVMDiagnosticsFailureType.Value getFailureType() {
        return UsageProto.CDPVMDiagnosticsFailureType.Value.INITIALIZATION_FAILURE;
    }

    @Override
    public String getOperationName() {
        return "Initialization";
    }

    @Override
    public String selector() {
        return INIT_DIAGNOSTICS_EVENT.selector();
    }
}
