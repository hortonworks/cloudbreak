package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.SALT_VALIDATION_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsSaltValidationHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsSaltValidationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    protected DiagnosticsSaltValidationHandler(EventSender eventSender) {
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
            Set<String> unresponsiveHosts = diagnosticsFlowService.collectUnresponsiveNodes(resourceId,
                    parameters.getHosts(), parameters.getHostGroups(), parameters.getExcludeHosts());
            LOGGER.debug("Diagnostics collection salt validation operation has been started. resourceCrn: '{}', parameters: '{}'",
                    resourceCrn, parameterMap);
            if (CollectionUtils.isNotEmpty(unresponsiveHosts)) {
                if (parameters.getSkipUnresponsiveHosts()) {
                    parameters.getExcludeHosts().addAll(unresponsiveHosts);
                    parameterMap = parameters.toMap();
                    LOGGER.debug("Diagnostics parameters has been updated with excluded hosts. resourceCrn: '{}', parameters: '{}'",
                            resourceCrn, parameterMap);
                } else {
                    throw new CloudbreakOrchestratorFailedException(
                            String.format("Some of the hosts are unresponsive, check the states of salt-minions on the following nodes: %s",
                                    StringUtils.join(unresponsiveHosts, ',')));
                }
            }
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_PREFLIGHT_CHECK_EVENT.selector())
                    .withParameters(parameters)
                    .withHosts(parameters.getHosts())
                    .withHostGroups(parameters.getHostGroups())
                    .withExcludeHosts(parameters.getExcludeHosts())
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics salt validation failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return SALT_VALIDATION_DIAGNOSTICS_EVENT.selector();
    }
}
