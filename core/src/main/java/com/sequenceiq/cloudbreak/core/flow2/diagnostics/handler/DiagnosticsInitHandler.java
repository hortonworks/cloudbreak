package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.INIT_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsInitHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsInitHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    public DiagnosticsInitHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public void accept(Event<DiagnosticsCollectionEvent> event) {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        if (StringUtils.isBlank(parameters.getUuid())) {
            LOGGER.debug("UUID is empty for diagnostics... Use flow ID as UUID.");
            parameters.setUuid(event.getHeaders().get(FlowConstants.FLOW_ID));
        }
        Map<String, Object> parameterMap = parameters.toMap();
        try {
            LOGGER.debug("Diagnostics collection initialization started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
            Set<String> hosts = data.getHosts();
            Set<String> hostGroups = data.getHostGroups();
            diagnosticsFlowService.init(resourceId, parameterMap, hosts, hostGroups);
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_ENSURE_MACHINE_USER_EVENT.selector())
                    .withParameters(parameters)
                    .withHosts(hosts)
                    .withHostGroups(hostGroups)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics collection initialization failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return INIT_DIAGNOSTICS_EVENT.selector();
    }
}
