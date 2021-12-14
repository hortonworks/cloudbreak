package com.sequenceiq.cloudbreak.core.flow2.diagnostics.handler;

import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionHandlerSelectors.CLEANUP_DIAGNOSTICS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionStateSelectors.FINISH_DIAGNOSTICS_COLLECTION_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.cloudbreak.core.flow2.diagnostics.event.DiagnosticsCollectionFailureEvent;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsCleanupHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsCleanupHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    protected DiagnosticsCleanupHandler(EventSender eventSender) {
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
            LOGGER.debug("Diagnostics cleanup started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameterMap);
            Set<String> hosts = parameters.getHosts();
            Set<String> hostGroups = parameters.getHostGroups();
            Set<String> excludedHosts = parameters.getExcludeHosts();
            diagnosticsFlowService.cleanup(resourceId, parameterMap, hosts, hostGroups, excludedHosts);
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(FINISH_DIAGNOSTICS_COLLECTION_EVENT.selector())
                    .withParameters(parameters)
                    .withHosts(hosts)
                    .withHostGroups(hostGroups)
                    .withExcludeHosts(excludedHosts)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics cleanup failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameterMap, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters,
                    UsageProto.CDPVMDiagnosticsFailureType.Value.CLEANUP_FAILURE.name());
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return CLEANUP_DIAGNOSTICS_EVENT.selector();
    }
}
