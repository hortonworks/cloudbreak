package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionHandlerSelectors.UPLOAD_CM_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_CLEANUP_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionFailureEvent;
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
    private DiagnosticsFlowService diagnosticsFlowService;

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
            diagnosticsFlowService.upload(resourceId, parameterMap);
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
