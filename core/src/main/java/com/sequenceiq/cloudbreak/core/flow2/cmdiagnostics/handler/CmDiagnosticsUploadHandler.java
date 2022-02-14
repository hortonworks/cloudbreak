package com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionHandlerSelectors.UPLOAD_CM_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_CLEANUP_EVENT;

import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.CmDiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.cmdiagnostics.event.CmDiagnosticsCollectionFailureEvent;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CmDiagnosticsUploadHandler extends EventSenderAwareHandler<CmDiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmDiagnosticsUploadHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CmDiagnosticsFlowService cmDiagnosticsFlowService;

    public CmDiagnosticsUploadHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return UPLOAD_CM_DIAGNOSTICS_EVENT.selector();
    }

    @Override
    public void accept(Event<CmDiagnosticsCollectionEvent> event) {
        CmDiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        CmDiagnosticsParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        try {
            LOGGER.debug("CM based diagnostics upload started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
            if (DiagnosticsDestination.SUPPORT.equals(parameters.getDestination())) {
                LOGGER.debug("CM based diagnostics uses SUPPORT destination, no support specific upload step yet.");
            } else {
                cmDiagnosticsFlowService.upload(resourceId, parameterMap, new HashSet<>());
            }
            CmDiagnosticsCollectionEvent diagnosticsCollectionEvent = CmDiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_CM_DIAGNOSTICS_CLEANUP_EVENT.selector())
                    .withParameters(parameters)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("CM based diagnostics upload failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            CmDiagnosticsCollectionFailureEvent failureEvent = new CmDiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }
}
