package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class DiagnosticsPreFlightCheckHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPreFlightCheckHandler.class);

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    public DiagnosticsPreFlightCheckHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector();
    }

    @Override
    public void accept(Event<DiagnosticsCollectionEvent> event) {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        diagnosticsFlowService.nodeStatusNetworkReport(resourceId);
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(parameters)
                .withHosts(parameters.getHosts())
                .withHostGroups(parameters.getHostGroups())
                .withExcludeHosts(parameters.getExcludeHosts())
                .build();
        eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
    }
}
