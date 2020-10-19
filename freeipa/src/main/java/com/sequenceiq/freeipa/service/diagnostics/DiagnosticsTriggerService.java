package com.sequenceiq.freeipa.service.diagnostics;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.telemetry.converter.DiagnosticsDataToParameterConverter;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class DiagnosticsTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsTriggerService.class);

    private static final String FREEIPA_CLUSTER_TYPE = "FREEIPA";

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private DiagnosticsDataToParameterConverter diagnosticsDataToParameterConverter;

    public FlowIdentifier startDiagnosticsCollection(DiagnosticsCollectionRequest request, String accountId, String userCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting diagnostics collection for FreeIpa. Crn: '{}'", stack.getResourceCrn());
        DiagnosticParameters parameters = diagnosticsDataToParameterConverter.convert(request, stack.getTelemetry(), FREEIPA_CLUSTER_TYPE,
                stack.getAppVersion(), stack.getAccountId(), stack.getRegion());
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(parameters)
                .build();
        return flowManager.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    private Event.Headers getFlowHeaders(String userCrn) {
        return new Event.Headers(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
    }
}
