package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.ENSURE_MACHINE_USER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsEnsureMachineUserHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsEnsureMachineUserHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private AltusMachineUserService altusMachineUserService;

    protected DiagnosticsEnsureMachineUserHandler(EventSender eventSender) {
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
            LOGGER.debug("Diagnostics collection ensure machine user operation started. resourceCrn: '{}', parameters: '{}'",
                    resourceCrn, parameterMap);
            if (DiagnosticsDestination.SUPPORT.equals(parameters.getDestination())) {
                LOGGER.debug("Generating databus credential if required for diagnostics support destination.");
                DataBusCredential credential = altusMachineUserService.getOrCreateDataBusCredentialIfNeeded(resourceId);
                parameters.setSupportBundleDbusAccessKey(credential.getAccessKey());
                parameters.setSupportBundleDbusPrivateKey(credential.getPrivateKey());
            }
            Set<String> hosts = parameters.getHosts();
            Set<String> hostGroups = parameters.getHostGroups();
            Set<String> excludedHosts = parameters.getExcludeHosts();
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_COLLECTION_EVENT.selector())
                    .withParameters(parameters)
                    .withHostGroups(hostGroups)
                    .withHosts(hosts)
                    .withExcludeHosts(excludedHosts)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics collection ensure machine user operation failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return ENSURE_MACHINE_USER_EVENT.selector();
    }
}
