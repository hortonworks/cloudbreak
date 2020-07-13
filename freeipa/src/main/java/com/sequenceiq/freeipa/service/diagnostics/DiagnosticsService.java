package com.sequenceiq.freeipa.service.diagnostics;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.freeipa.converter.diagnostics.DiagnosticsDataToMapConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.rx.Promise;

@Service
public class DiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsService.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private DiagnosticsDataToMapConverter diagnosticsDataToMapConverter;

    public void startDiagnosticsCollection(DiagnosticsCollectionRequest request, String accountId, String userCrn) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting diagnostics collection for FreeIpa. Crn: '{}'", stack.getResourceCrn());
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withAccepted(new Promise<>())
                .withResourceId(stack.getId())
                .withResourceCrn(stack.getResourceCrn())
                .withSelector(DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(diagnosticsDataToMapConverter.convert(request, stack))
                .build();
        flowManager.notify(diagnosticsCollectionEvent, getFlowHeaders(userCrn));
    }

    private Event.Headers getFlowHeaders(String userCrn) {
        return new Event.Headers(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn));
    }
}
