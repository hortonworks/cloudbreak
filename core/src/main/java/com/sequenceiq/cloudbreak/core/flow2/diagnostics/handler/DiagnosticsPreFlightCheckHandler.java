package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DiagnosticsPreFlightCheckHandler extends AbstractDiagnosticsOperationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPreFlightCheckHandler.class);

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    @Inject
    private StackService stackService;

    @Override
    public Selectable executeOperation(HandlerEvent<DiagnosticsCollectionEvent> event) throws Exception {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
        diagnosticsFlowService.nodeStatusNetworkReport(stack);
        return DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_INIT_EVENT.selector())
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
        return "Pre-flight check";
    }

    @Override
    public String selector() {
        return PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector();
    }
}
