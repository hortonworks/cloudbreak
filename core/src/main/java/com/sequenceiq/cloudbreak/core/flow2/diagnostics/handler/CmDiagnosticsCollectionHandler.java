package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionHandlerSelectors.COLLECT_CM_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionStateSelectors.START_CM_DIAGNOSTICS_UPLOAD_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.CmDiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDiagnosticsService;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CmDiagnosticsCollectionHandler extends EventSenderAwareHandler<CmDiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmDiagnosticsCollectionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterDiagnosticsService clusterDiagnosticsService;

    public CmDiagnosticsCollectionHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return COLLECT_CM_DIAGNOSTICS_EVENT.selector();
    }

    @Override
    public void accept(Event<CmDiagnosticsCollectionEvent> event) {
        CmDiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        CmDiagnosticsParameters parameters = data.getParameters();
        Map<String, Object> parameterMap = parameters.toMap();
        try {
            LOGGER.debug("CM based diagnostics collection started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
            clusterDiagnosticsService.collectDiagnostics(resourceId, parameters);
            CmDiagnosticsCollectionEvent diagnosticsCollectionEvent = CmDiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_CM_DIAGNOSTICS_UPLOAD_EVENT.selector())
                    .withParameters(parameters)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("CM based diagnostics collection failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            CmDiagnosticsCollectionFailureEvent failureEvent = new CmDiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }
}
