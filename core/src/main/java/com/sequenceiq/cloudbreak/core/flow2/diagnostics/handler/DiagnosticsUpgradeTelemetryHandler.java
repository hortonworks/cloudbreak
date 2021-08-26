package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.UPGRADE_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsUpgradeTelemetryHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsUpgradeTelemetryHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    public DiagnosticsUpgradeTelemetryHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public void accept(Event<DiagnosticsCollectionEvent> event) {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        try {
            diagnosticsFlowService.telemetryUpgrade(resourceId, parameterMap, parameters.getHosts(),
                    parameters.getHostGroups(), parameters.getExcludeHosts(), true);
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_VM_PREFLIGHT_CHECK_EVENT.selector())
                    .withParameters(parameters)
                    .withHosts(parameters.getHosts())
                    .withHostGroups(parameters.getHostGroups())
                    .withExcludeHosts(parameters.getExcludeHosts())
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics upgrade cdp-telemetry failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return UPGRADE_DIAGNOSTICS_EVENT.selector();
    }
}
